package com.liveklass.common.exception;

import java.util.List;

public record ErrorResponse(
	String code,
	String message,
	List<FieldErrorResponse> fieldErrors
) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message, List.of());
	}

	public static ErrorResponse withFieldErrors(
		ErrorCode errorCode,
		String message,
		List<FieldErrorResponse> fieldErrors
	) {
		return new ErrorResponse(errorCode.name(), message, fieldErrors);
	}
}
