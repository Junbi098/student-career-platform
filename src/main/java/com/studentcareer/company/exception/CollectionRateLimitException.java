package com.studentcareer.company.exception;

public class CollectionRateLimitException extends RuntimeException {
    private final long retryAfter;

    public CollectionRateLimitException(long retryAfter) {
        super("Wait before starting another real-source import");
        this.retryAfter = retryAfter;
    }

    public long getRetryAfter() {
        return retryAfter;
    }
}
