package dev.lotr.sdk.http;

/** HTTP status code constants used by the SDK. */
public final class HttpStatus {

    public static final int OK = 200;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int TOO_MANY_REQUESTS = 429;

    private HttpStatus() {}
}
