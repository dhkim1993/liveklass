package com.liveklass.payment.command.service;

import java.math.BigDecimal;

public record PaymentCallbackDto(
	String paymentId,
	Long enrollmentId,
	BigDecimal paidAmount,
	String idempotencyKey
) {

	public String resolvedIdempotencyKey() {
		if (idempotencyKey != null && !idempotencyKey.isBlank()) {
			return idempotencyKey;
		}
		return "payment:" + paymentId;
	}
}
