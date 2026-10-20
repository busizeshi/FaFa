package com.fafa.common.web;

import java.util.UUID;
import java.util.regex.Pattern;

/** Generates and validates request IDs before they enter logs or downstream headers. */
public final class RequestIdContext {
    /** Header used to send a request ID between Gateway and backend services. */
    public static final String HEADER_NAME = "X-Request-ID";
    /** MDC key used by Logback patterns. */
    public static final String MDC_KEY = "requestId";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");

    private RequestIdContext() {
    }

    /** Returns a validated caller ID or creates a new UUID when the value is absent or unsafe.
     * @param candidate caller-provided request ID
     * @return validated request ID
     */
    public static String resolve(String candidate) {
        if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
