package com.onec.guesty;

/**
 * Raised when the Guesty Open API returns a non-2xx status. Carries the HTTP {@link #status()} and
 * the raw response {@link #body()} so callers can branch on, e.g., 404 (not found) versus 422
 * (validation) without re-parsing.
 */
public class GuestyApiException extends GuestyException {

    private final int status;
    private final String body;

    public GuestyApiException(int status, String body) {
        super("Guesty API returned HTTP " + status + (body == null || body.isBlank() ? "" : ": " + body));
        this.status = status;
        this.body = body;
    }

    public int status() {
        return status;
    }

    public String body() {
        return body;
    }

    /** Whether the credentials/token were rejected (HTTP 401). */
    public boolean isUnauthorized() {
        return status == 401;
    }

    /** Whether the account lacks permission for the resource (HTTP 403). */
    public boolean isForbidden() {
        return status == 403;
    }

    public boolean isNotFound() {
        return status == 404;
    }

    /** Whether the request was rate limited (HTTP 429). */
    public boolean isRateLimited() {
        return status == 429;
    }
}
