package com.liveklass.payment.command.service;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.payment.command.repository.PaymentIdempotencyRepository;
import com.liveklass.payment.domain.PaymentIdempotency;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentIdempotencyService {

	private final PaymentIdempotencyRepository paymentIdempotencyRepository;

	public PaymentIdempotency start(String idempotencyKey, String requestHash) {
		return paymentIdempotencyRepository.findByIdempotencyKey(idempotencyKey)
			.map(idempotency -> validateExisting(idempotency, requestHash))
			.orElseGet(() -> createProcessing(idempotencyKey, requestHash));
	}

	private PaymentIdempotency validateExisting(PaymentIdempotency idempotency, String requestHash) {
		idempotency.validateSameRequest(requestHash);
		if (idempotency.isCompleted()) {
			return idempotency;
		}
		idempotency.validateNotProcessing();
		return idempotency;
	}

	private PaymentIdempotency createProcessing(String idempotencyKey, String requestHash) {
		try {
			return paymentIdempotencyRepository.saveAndFlush(
				PaymentIdempotency.processing(idempotencyKey, requestHash)
			);
		} catch (DataIntegrityViolationException exception) {
			throw new LiveKlassException(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);
		}
	}
}
