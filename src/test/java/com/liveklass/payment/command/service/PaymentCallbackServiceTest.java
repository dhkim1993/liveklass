package com.liveklass.payment.command.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.infrastructure.outbox.domain.OutboxEvent;
import com.liveklass.infrastructure.outbox.domain.enums.OutboxEventType;
import com.liveklass.infrastructure.outbox.repository.OutboxEventRepository;
import com.liveklass.enrollment.command.repository.EnrollmentRepository;
import com.liveklass.enrollment.command.service.EnrollmentCommandService;
import com.liveklass.enrollment.domain.Enrollment;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.klass.command.repository.KlassRepository;
import com.liveklass.klass.command.service.CreateKlassDto;
import com.liveklass.klass.command.service.KlassCommandService;
import com.liveklass.payment.command.repository.PaymentIdempotencyRepository;
import com.liveklass.payment.domain.PaymentIdempotency;
import com.liveklass.payment.domain.enums.PaymentIdempotencyStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class PaymentCallbackServiceTest {

	private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final Instant FIXED_INSTANT = Instant.parse("2026-07-12T00:00:00Z");
	private static final LocalDateTime FIXED_NOW = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE_ID);
	private static final Long CREATOR_ID = 1L;
	private static final Long USER_ID = 2L;
	private static final BigDecimal PRICE = BigDecimal.valueOf(10000);

	@Autowired
	private KlassCommandService klassCommandService;

	@Autowired
	private EnrollmentCommandService enrollmentCommandService;

	@Autowired
	private PaymentCallbackService paymentCallbackService;

	@Autowired
	private KlassRepository klassRepository;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@Autowired
	private PaymentIdempotencyRepository paymentIdempotencyRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@BeforeEach
	void setUp() {
		outboxEventRepository.deleteAll();
		paymentIdempotencyRepository.deleteAll();
		enrollmentRepository.deleteAll();
		klassRepository.deleteAll();
	}

	@Test
	void confirmPaymentChangesEnrollmentStatusAndSavesOutboxEvent() {
		Long enrollmentId = createPendingEnrollment();

		paymentCallbackService.confirm(callback("pay-1", enrollmentId, PRICE, "key-1"));

		Enrollment enrollment = enrollmentRepository.getByIdOrThrow(enrollmentId);
		assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
		assertThat(enrollment.getConfirmedAt()).isEqualTo(FIXED_NOW);

		assertThat(outboxEventRepository.findAll())
			.extracting(OutboxEvent::getEventType)
			.containsExactlyInAnyOrder(
				OutboxEventType.ENROLLMENT_CREATED,
				OutboxEventType.ENROLLMENT_CONFIRMED
			);
		assertThat(paymentIdempotencyRepository.findAll())
			.extracting(PaymentIdempotency::getStatus)
			.containsExactly(PaymentIdempotencyStatus.COMPLETED);
	}

	@Test
	void sameKeyAndSamePayloadReturnsIdempotently() {
		Long enrollmentId = createPendingEnrollment();
		PaymentCallbackDto callback = callback("pay-1", enrollmentId, PRICE, "key-1");

		paymentCallbackService.confirm(callback);
		paymentCallbackService.confirm(callback);

		assertThat(outboxEventRepository.findAll())
			.extracting(OutboxEvent::getEventType)
			.containsExactlyInAnyOrder(
				OutboxEventType.ENROLLMENT_CREATED,
				OutboxEventType.ENROLLMENT_CONFIRMED
			);
		assertThat(paymentIdempotencyRepository.findAll()).hasSize(1);
	}

	@Test
	void sameKeyAndDifferentPayloadIsRejected() {
		Long enrollmentId = createPendingEnrollment();
		paymentCallbackService.confirm(callback("pay-1", enrollmentId, PRICE, "key-1"));

		assertThatThrownBy(() -> paymentCallbackService.confirm(
			callback("pay-2", enrollmentId, PRICE, "key-1")
		))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT)
			);
	}

	@Test
	void processingSameRequestIsRejected() {
		Long enrollmentId = createPendingEnrollment();
		PaymentCallbackDto callback = callback("pay-1", enrollmentId, PRICE, "processing-key");
		paymentIdempotencyRepository.saveAndFlush(
			PaymentIdempotency.processing(callback.resolvedIdempotencyKey(), hash(callback))
		);

		assertThatThrownBy(() -> paymentCallbackService.confirm(callback))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING)
			);
	}

	@Test
	void invalidPaymentAmountIsRejected() {
		Long enrollmentId = createPendingEnrollment();

		assertThatThrownBy(() -> paymentCallbackService.confirm(
			callback("pay-1", enrollmentId, BigDecimal.valueOf(9000), "key-1")
		))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_AMOUNT)
			);

		Enrollment enrollment = enrollmentRepository.getByIdOrThrow(enrollmentId);
		assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
	}

	@Test
	void alreadyConfirmedEnrollmentReturnsIdempotently() {
		Long enrollmentId = createPendingEnrollment();
		paymentCallbackService.confirm(callback("pay-1", enrollmentId, PRICE, "key-1"));

		paymentCallbackService.confirm(callback("pay-2", enrollmentId, PRICE, "key-2"));

		assertThat(outboxEventRepository.findAll())
			.extracting(OutboxEvent::getEventType)
			.containsExactlyInAnyOrder(
				OutboxEventType.ENROLLMENT_CREATED,
				OutboxEventType.ENROLLMENT_CONFIRMED
			);
		assertThat(paymentIdempotencyRepository.findAll()).hasSize(2);
	}

	@Test
	void missingIdempotencyKeyUsesPaymentIdFallback() {
		Long enrollmentId = createPendingEnrollment();

		paymentCallbackService.confirm(callback("pay-1", enrollmentId, PRICE, null));

		PaymentIdempotency idempotency = paymentIdempotencyRepository.findAll().get(0);
		assertThat(idempotency.getIdempotencyKey()).isEqualTo("payment:pay-1");
		assertThat(idempotency.getStatus()).isEqualTo(PaymentIdempotencyStatus.COMPLETED);
	}

	private Long createPendingEnrollment() {
		Long klassId = createOpenKlass();
		return enrollmentCommandService.enroll(klassId, USER_ID);
	}

	private Long createOpenKlass() {
		LocalDateTime startDate = FIXED_NOW.plusDays(30);
		Long klassId = klassCommandService.create(new CreateKlassDto(
			CREATOR_ID,
			"Payment test",
			"Payment test description",
			PRICE,
			10,
			startDate,
			startDate.plusDays(30)
		));
		klassCommandService.open(klassId, CREATOR_ID);
		return klassId;
	}

	private PaymentCallbackDto callback(
		String paymentId,
		Long enrollmentId,
		BigDecimal paidAmount,
		String idempotencyKey
	) {
		return new PaymentCallbackDto(paymentId, enrollmentId, paidAmount, idempotencyKey);
	}

	private String hash(PaymentCallbackDto dto) {
		String payload = dto.paymentId() + ":" + dto.enrollmentId() + ":" + dto.paidAmount().toPlainString();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(FIXED_INSTANT, ZONE_ID);
		}
	}
}
