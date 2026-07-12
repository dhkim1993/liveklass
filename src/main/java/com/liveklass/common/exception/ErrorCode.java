package com.liveklass.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	KLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
	ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청을 찾을 수 없습니다."),

	KLASS_NOT_OPEN(HttpStatus.CONFLICT, "모집 중인 강의만 신청할 수 있습니다."),
	KLASS_ALREADY_OPEN(HttpStatus.CONFLICT, "이미 모집 중인 강의입니다."),
	KLASS_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 모집 마감된 강의입니다."),
	KLASS_CANNOT_OPEN(HttpStatus.CONFLICT, "초안 상태의 강의만 모집 시작할 수 있습니다."),
	KLASS_CANNOT_CLOSE(HttpStatus.CONFLICT, "모집 중인 강의만 모집 마감할 수 있습니다."),
	CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "수강 정원을 초과할 수 없습니다."),
	DUPLICATE_ACTIVE_ENROLLMENT(HttpStatus.CONFLICT, "이미 활성 상태의 수강 신청이 존재합니다."),
	INVALID_ENROLLMENT_STATUS(HttpStatus.CONFLICT, "현재 신청 상태에서는 처리할 수 없습니다."),
	CANCEL_PERIOD_EXPIRED(HttpStatus.CONFLICT, "취소 가능 기간이 지났습니다."),
	CONCURRENCY_CONFLICT(HttpStatus.CONFLICT, "동시 요청 충돌이 발생했습니다. 다시 시도해 주세요."),

	FORBIDDEN_KLASS_ACCESS(HttpStatus.FORBIDDEN, "강의에 접근할 권한이 없습니다."),
	FORBIDDEN_ENROLLMENT_ACCESS(HttpStatus.FORBIDDEN, "수강 신청에 접근할 권한이 없습니다."),

	INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 강의 가격과 일치하지 않습니다."),
	IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "같은 멱등성 키로 다른 요청이 들어왔습니다."),
	IDEMPOTENCY_REQUEST_PROCESSING(HttpStatus.CONFLICT, "같은 멱등성 키의 요청이 처리 중입니다."),

	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;
}
