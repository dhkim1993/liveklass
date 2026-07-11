package com.liveklass.common.exception;

import lombok.Getter;

@Getter
public class LiveKlassException extends RuntimeException {

	private final ErrorCode errorCode;

	public LiveKlassException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public LiveKlassException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
