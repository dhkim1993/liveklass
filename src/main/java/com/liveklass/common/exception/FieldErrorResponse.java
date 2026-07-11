package com.liveklass.common.exception;

public record FieldErrorResponse(
	String field,
	String message
) {
}
