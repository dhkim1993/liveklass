package com.liveklass.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(LiveKlassException.class)
	public ResponseEntity<ErrorResponse> handleLiveKlassException(LiveKlassException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ErrorResponse.of(errorCode, exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException exception
	) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::toFieldErrorResponse)
			.toList();

		return ResponseEntity
			.badRequest()
			.body(ErrorResponse.withFieldErrors(
				ErrorCode.INVALID_REQUEST,
				ErrorCode.INVALID_REQUEST.getMessage(),
				fieldErrors
			));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(
		ConstraintViolationException exception
	) {
		List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations()
			.stream()
			.map(violation -> new FieldErrorResponse(
				violation.getPropertyPath().toString(),
				violation.getMessage()
			))
			.toList();

		return ResponseEntity
			.badRequest()
			.body(ErrorResponse.withFieldErrors(
				ErrorCode.INVALID_REQUEST,
				ErrorCode.INVALID_REQUEST.getMessage(),
				fieldErrors
			));
	}

	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	public ResponseEntity<ErrorResponse> handleIllegalException(RuntimeException exception) {
		return ResponseEntity
			.badRequest()
			.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, exception.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		return ResponseEntity
			.internalServerError()
			.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	private FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
		return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
	}
}
