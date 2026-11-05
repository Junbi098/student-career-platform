package com.studentcareer.source.reader;

import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.model.SourceVerificationStatus;
import com.studentcareer.support.WikidataTestData;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WikidataSourceReaderTest {
    private HttpServer server;
    private URI uri;
    private byte[] payload = WikidataTestData.document("Q3301671").toString().getBytes(StandardCharsets.UTF_8);
    private int status = 200;
    private String type = "application/json; charset=UTF-8";
    private String encoding;
    private String retryAfter;
    private long delay;
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicInteger redirectHits = new AtomicInteger();
    private final Instant now = Instant.parse("2026-11-05T00:00:00Z");
    private String userAgent;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/Q3301671.json", exchange -> {
            hits.incrementAndGet();
            userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            try {
                if (delay > 0) { Thread.sleep(delay); }
                exchange.getResponseHeaders().set("Content-Type", type);
                if (encoding != null) { exchange.getResponseHeaders().set("Content-Encoding", encoding); }
                if (retryAfter != null) { exchange.getResponseHeaders().set("Retry-After", retryAfter); }
                if (status == 302) { exchange.getResponseHeaders().set("Location", "/redirect-target"); }
                exchange.sendResponseHeaders(status, 0); // Chunked response exercises streaming size checks.
                exchange.getResponseBody().write(payload);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // Client cancellation is expected during timeout/size-limit tests.
            } finally {
                exchange.close();
            }
        });
        server.createContext("/redirect-target", exchange -> {
            redirectHits.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/Q3301671.json");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private WikidataSourceReader reader(Duration timeout, int maxBytes) {
        return new WikidataSourceReader(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                Clock.fixed(now, ZoneOffset.UTC), Set.of(uri), timeout, Duration.ZERO, maxBytes);
    }

    private WikidataSourceReader reader() {
        return reader(Duration.ofSeconds(3), 524288);
    }

    @Test
    void shouldRecordActualUriReadTimeSourceTimeLicenseAndUnverifiedStatus() throws Exception {
        var result = reader().readSource(uri, SourceKind.WEB_OBSERVATION);
        assertEquals(uri, result.source().getUrl());
        assertEquals(now, result.source().getAccessedAt());
        assertEquals(Instant.parse("2020-01-02T03:04:05Z"), result.source().getSourceUpdatedAt());
        assertEquals("CC0-1.0", result.source().getLicense());
        assertEquals(SourceVerificationStatus.UNVERIFIED, result.source().getVerificationStatus());
        assertTrue(result.content().contains("Synthetic Island Network"));
        assertTrue(userAgent.startsWith("StudentCareerPlatform/"));
    }

    @Test
    void shouldRejectUnregisteredUriAndWrongCategoryBeforeAnyRequest() {
        var reader = reader();
        assertThrows(IllegalArgumentException.class,
                () -> reader.readSource(URI.create("http://127.0.0.1/private"), SourceKind.WEB_OBSERVATION));
        assertThrows(IllegalArgumentException.class, () -> reader.readSource(uri, SourceKind.FIXTURE));
        assertEquals(0, hits.get());
    }

    @Test
    void shouldRefuseRedirectWithoutFollowingIt() {
        status = 302;
        assertThrows(IOException.class, () -> reader().readSource(uri, SourceKind.WEB_OBSERVATION));
        assertEquals(0, redirectHits.get());
    }

    @Test
    void shouldReportHttpFailure() {
        status = 404;
        assertThrows(IOException.class, () -> reader().readSource(uri, SourceKind.WEB_OBSERVATION));
    }

    @Test
    void shouldPauseFurtherRequestsAfter429() {
        status = 429;
        retryAfter = "120";
        var reader = reader();
        assertThrows(IOException.class, () -> reader.readSource(uri, SourceKind.WEB_OBSERVATION));
        assertThrows(IOException.class, () -> reader.readSource(uri, SourceKind.WEB_OBSERVATION));
        assertEquals(1, hits.get());
    }

    @Test
    void shouldHonorRetryAfterHttpDateFor503() {
        status = 503;
        retryAfter = "Thu, 05 Nov 2026 00:10:00 GMT";
        var reader = reader();
        assertThrows(IOException.class, () -> reader.readSource(uri, SourceKind.WEB_OBSERVATION));
        assertThrows(IOException.class, () -> reader.readSource(uri, SourceKind.WEB_OBSERVATION));
        assertEquals(1, hits.get());
    }

    @Test
    void shouldRejectWrongContentTypeAndMalformedJson() {
        type = "text/html";
        assertThrows(IOException.class, () -> reader().readSource(uri, SourceKind.WEB_OBSERVATION));
        type = "application/json";
        payload = "Not JSON".getBytes(StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> reader().readSource(uri, SourceKind.WEB_OBSERVATION));
    }

    @Test
    void shouldRejectPayloadForAnotherEntity() {
        payload = WikidataTestData.document("Q2").toString().getBytes(StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> reader().readSource(uri, SourceKind.WEB_OBSERVATION));
    }

    @Test
    void shouldBoundChunkedResponseSize() {
        assertThrows(IOException.class, () -> reader(Duration.ofSeconds(3), 60)
                .readSource(uri, SourceKind.WEB_OBSERVATION));
    }

    @Test
    void shouldDecodeGzipAndBoundDecompressedSize() throws Exception {
        encoding = "gzip";
        try (var buffer = new ByteArrayOutputStream()) {
            try (var gzip = new GZIPOutputStream(buffer)) { gzip.write(payload); }
            payload = buffer.toByteArray();
        }
        assertNotNull(reader().readSource(uri, SourceKind.WEB_OBSERVATION));
        assertThrows(IOException.class, () -> reader(Duration.ofSeconds(3), 256)
                .readSource(uri, SourceKind.WEB_OBSERVATION));
    }

    @Test
    void shouldTimeoutAndCancelSlowRequest() {
        delay = 500;
        assertThrows(IOException.class, () -> reader(Duration.ofMillis(100), 524288)
                .readSource(uri, SourceKind.WEB_OBSERVATION));
    }

    @Test
    void shouldKeepMissingSourceDateUnknown() throws Exception {
        var data = WikidataTestData.document("Q3301671");
        ((com.fasterxml.jackson.databind.node.ObjectNode) data.path("entities").path("Q3301671")).remove("modified");
        payload = data.toString().getBytes(StandardCharsets.UTF_8);
        assertNull(reader().readSource(uri, SourceKind.WEB_OBSERVATION).source().getSourceUpdatedAt());
    }
    @Test
    void shouldStopFurtherRequestsWhenProviderDeniesAccess() {
        status = 403;
        var reader = reader();
        assertThrows(IOException.class, () -> reader.readSource(uri, SourceKind.WEB_OBSERVATION));
        assertThrows(IOException.class, () -> reader.readSource(uri, SourceKind.WEB_OBSERVATION));
        assertEquals(1, hits.get());
    }
}
