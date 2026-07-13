package com.liveklass.payment.command.controller.request;

import com.liveklass.payment.command.service.PaymentCallbackDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PaymentCallbackRequest(
	@NotBlank String paymentId,
	@NotNull @Positive Long enrollmentId,
	@NotNull @Positive BigDecimal paidAmount
) {

	public PaymentCallbackDto toDto(String idempotencyKey) {
		return new PaymentCallbackDto(paymentId, enrollmentId, paidAmount, idempotencyKey);
	}
}
