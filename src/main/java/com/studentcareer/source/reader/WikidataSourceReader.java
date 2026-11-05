package com.studentcareer.source.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.source.model.SourceDocument;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.model.SourceReference;
import com.studentcareer.source.wikidata.WikidataSources;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

/** Small allowlisted JSON exports only; no crawling, redirects, credentials or automatic retries. */
public class WikidataSourceReader implements SourceReader {
    private static final int MAX_BYTES = 512 * 1024;
    private final HttpClient client;
    private final Clock clock;
    private final Set<URI> allowed;
    private final Duration timeout;
    private final Duration spacing;
    private final int maxBytes;
    private final ObjectMapper mapper = new ObjectMapper();
    private Instant blockedUntil = Instant.MIN;
    private long lastRequestNanos;

    public WikidataSourceReader() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER).build(), Clock.systemUTC(),
                WikidataSources.ALLOWED_URIS, Duration.ofSeconds(15), Duration.ofSeconds(1), MAX_BYTES);
    }

    // Package-private injection allows tests against an isolated local HTTP server, not the live source.
    WikidataSourceReader(HttpClient client, Clock clock, Set<URI> allowed,
                         Duration timeout, Duration spacing, int maxBytes) {
        if (client.followRedirects() != HttpClient.Redirect.NEVER || timeout.isNegative()
                || timeout.isZero() || spacing.isNegative() || maxBytes < 1) {
            throw new IllegalArgumentException("Invalid HTTP reader configuration");
        }
        this.client = client;
        this.clock = clock;
        this.allowed = Set.copyOf(allowed);
        this.timeout = timeout;
        this.spacing = spacing;
        this.maxBytes = maxBytes;
    }

    @Override
    public synchronized SourceDocument readSource(URI uri, SourceKind kind) throws IOException {
        if (kind != SourceKind.WEB_OBSERVATION || uri == null || !allowed.contains(uri)) {
            throw new IllegalArgumentException("Only registered Wikidata company JSON exports are allowed");
        }
        if (clock.instant().isBefore(blockedUntil)) {
            throw new IOException("Source cooldown active until " + blockedUntil);
        }
        pauseBetweenRequests();
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout)
                .header("User-Agent", WikidataSources.USER_AGENT)
                .header("Accept", "application/json").header("Accept-Encoding", "gzip").GET().build();
        var future = client.sendAsync(request, info -> new LimitedBodySubscriber(maxBytes));
        HttpResponse<byte[]> response;
        try {
            response = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new IOException("Source request timed out", exception);
        } catch (InterruptedException exception) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Source request interrupted");
        } catch (ExecutionException exception) {
            throw new IOException("Source request failed", exception.getCause());
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            blockedUntil = clock.instant().plusSeconds(3600);
            throw new IOException("Source denied access; collection paused");
        }
        if (response.statusCode() == 429 || response.statusCode() == 503) {
            blockedUntil = retryAt(response.headers().firstValue("Retry-After").orElse("60"));
            throw new IOException("Source temporarily unavailable; retry after " + blockedUntil);
        }
        if (response.statusCode() != 200) {
            throw new IOException("Source returned HTTP " + response.statusCode());
        }
        String type = response.headers().firstValue("Content-Type").orElse("")
                .split(";", 2)[0].strip();
        if (!"application/json".equalsIgnoreCase(type) && !"application/ld+json".equalsIgnoreCase(type)) {
            throw new IOException("Expected a JSON response from source");
        }
        byte[] bytes = response.body();
        String encoding = response.headers().firstValue("Content-Encoding").orElse("identity");
        if ("gzip".equalsIgnoreCase(encoding)) {
            try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                bytes = gzip.readNBytes(maxBytes + 1);
            }
        } else if (!"identity".equalsIgnoreCase(encoding)) {
            throw new IOException("Unsupported source content encoding");
        }
        if (bytes.length > maxBytes) {
            throw new IOException("Source body exceeds size limit");
        }
        String content = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        JsonNode root = mapper.readTree(content);
        if (root == null) {
            throw new IOException("Empty JSON response from source");
        }
        JsonNode entities = root.path("entities");
        String path = uri.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
        if (!entities.isObject() || entities.size() != 1 || !entities.has(id)
                || !id.equals(entities.path(id).path("id").asText())) {
            throw new IOException("Source payload does not match the requested entity");
        }
        Instant updatedAt = null;
        try {
            updatedAt = Instant.parse(entities.path(id).path("modified").asText());
        } catch (java.time.format.DateTimeParseException ignored) {
            // Optional source date: absence must not be replaced with the current time.
        }
        var source = new SourceReference(response.uri(), "Wikidata company entity " + id,
                clock.instant(), SourceKind.WEB_OBSERVATION, updatedAt, "CC0-1.0");
        return new SourceDocument(content, source);
    }

    private void pauseBetweenRequests() throws InterruptedIOException {
        long remaining = lastRequestNanos == 0 ? 0 : spacing.toNanos() - (System.nanoTime() - lastRequestNanos);
        if (remaining > 0) {
            try {
                TimeUnit.NANOSECONDS.sleep(remaining);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Source request interrupted");
            }
        }
        lastRequestNanos = System.nanoTime();
    }

    private Instant retryAt(String value) {
        Instant now = clock.instant();
        try {
            Instant date = now.plusSeconds(Math.max(60, Long.parseLong(value)));
            return date;
        } catch (RuntimeException ignored) {
            try {
                Instant date = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                return date.isAfter(now.plusSeconds(60)) ? date : now.plusSeconds(60);
            } catch (RuntimeException invalidDate) {
                return now.plusSeconds(60);
            }
        }
    }

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int maxBytes;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private Flow.Subscription subscription;

        LimitedBodySubscriber(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > maxBytes - output.size()) {
                    subscription.cancel();
                    body.completeExceptionally(new IOException("Source body exceeds size limit"));
                    return;
                }
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                output.writeBytes(bytes);
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            body.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            body.complete(output.toByteArray());
        }
    }
}
