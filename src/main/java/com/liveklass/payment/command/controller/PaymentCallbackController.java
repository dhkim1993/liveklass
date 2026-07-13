package com.liveklass.payment.command.controller;

import com.liveklass.payment.command.controller.request.PaymentCallbackRequest;
import com.liveklass.payment.command.service.PaymentCallbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentCallbackController {

	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

	private final PaymentCallbackService paymentCallbackService;

	// Simulates the external PG completion callback entry point for this assignment.
	@PostMapping("/callback")
	public ResponseEntity<Void> callback(
		@RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
		@Valid @RequestBody PaymentCallbackRequest request
	) {
		paymentCallbackService.confirm(request.toDto(idempotencyKey));
		return ResponseEntity.noContent().build();
	}
}
