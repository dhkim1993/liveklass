package com.liveklass.payment.command.service;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.infrastructure.outbox.domain.enums.OutboxEventType;
import com.liveklass.infrastructure.outbox.service.OutboxEventService;
import com.liveklass.enrollment.command.repository.EnrollmentRepository;
import com.liveklass.enrollment.domain.Enrollment;
import com.liveklass.payment.domain.PaymentIdempotency;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentCallbackService {

	private final EnrollmentRepository enrollmentRepository;
	private final PaymentIdempotencyService paymentIdempotencyService;
	private final OutboxEventService outboxEventService;
	private final Clock clock;

	// The PG payment itself is assumed to be completed before this callback is received.
	public void confirm(PaymentCallbackDto dto) {
		String requestHash = hash(dto);
		PaymentIdempotency idempotency = paymentIdempotencyService.start(
			dto.resolvedIdempotencyKey(),
			requestHash
		);
		if (idempotency.isCompleted()) {
			return;
		}

		Enrollment enrollment = enrollmentRepository.getByIdOrThrow(dto.enrollmentId());
		validatePaidAmount(dto.paidAmount(), enrollment.getKlass().getPrice());

		LocalDateTime now = LocalDateTime.now(clock);
		boolean confirmed = enrollment.confirm(now);
		if (confirmed) {
			outboxEventService.save(
				"Enrollment",
				enrollment.getId(),
				OutboxEventType.ENROLLMENT_CONFIRMED,
				toEnrollmentPayload(enrollment, dto.paymentId())
			);
		}
		idempotency.complete(now);
	}

	private void validatePaidAmount(BigDecimal paidAmount, BigDecimal price) {
		if (paidAmount.compareTo(price) != 0) {
			throw new LiveKlassException(ErrorCode.INVALID_PAYMENT_AMOUNT);
		}
	}

	private String hash(PaymentCallbackDto dto) {
		String payload = dto.paymentId() + ":" + dto.enrollmentId() + ":" + dto.paidAmount().toPlainString();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is not available", exception);
		}
	}

	private Map<String, Object> toEnrollmentPayload(Enrollment enrollment, String paymentId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("enrollmentId", enrollment.getId());
		payload.put("klassId", enrollment.getKlass().getId());
		payload.put("userId", enrollment.getUserId());
		payload.put("status", enrollment.getStatus().name());
		payload.put("paymentId", paymentId);
		return payload;
	}
}
