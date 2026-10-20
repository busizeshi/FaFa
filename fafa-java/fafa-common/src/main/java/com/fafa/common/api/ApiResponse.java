package com.fafa.common.api;

/** Stable HTTP response envelope shared by Java services.
 *
 * @param <T> payload type
 * @param code stable response code
 * @param message client-safe response message
 * @param data response payload or {@code null}
 * @param requestId request correlation ID
 */
public record ApiResponse<T>(String code, String message, T data, String requestId) {

    /** Creates a successful response with the supplied payload and request ID.
     *
     * @param <T> payload type
     * @param data successful response payload
     * @param requestId request correlation ID
     * @return a success envelope
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>("0", "OK", data, requestId);
    }

    /** Creates an error response while keeping the same wire shape as success responses.
     *
     * @param <T> payload type
     * @param code stable error code
     * @param message client-safe error message
     * @param data optional safe error details
     * @param requestId request correlation ID
     * @return an error envelope
     */
    public static <T> ApiResponse<T> failure(String code, String message, T data, String requestId) {
        return new ApiResponse<>(code, message, data, requestId);
    }
}
