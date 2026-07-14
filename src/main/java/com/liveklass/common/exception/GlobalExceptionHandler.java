package com.liveklass.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
		MissingRequestHeaderException exception
	) {
		FieldErrorResponse fieldError = new FieldErrorResponse(
			exception.getHeaderName(),
			"필수 요청 헤더가 누락되었습니다."
		);

		return ResponseEntity
			.badRequest()
			.body(ErrorResponse.withFieldErrors(
				ErrorCode.INVALID_REQUEST,
				ErrorCode.INVALID_REQUEST.getMessage(),
				List.of(fieldError)
			));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException exception
	) {
		FieldErrorResponse fieldError = new FieldErrorResponse(
			exception.getName(),
			"요청 값의 타입이 올바르지 않습니다."
		);

		return ResponseEntity
			.badRequest()
			.body(ErrorResponse.withFieldErrors(
				ErrorCode.INVALID_REQUEST,
				ErrorCode.INVALID_REQUEST.getMessage(),
				List.of(fieldError)
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
