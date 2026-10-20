package com.fafa.common.error;

/** Validation error without echoing submitted field values.
 *
 * @param field name or path of the invalid field
 * @param message client-safe validation message
 */
public record FieldViolation(String field, String message) {
}
