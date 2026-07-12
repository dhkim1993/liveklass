package com.liveklass.enrollment.commandservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.commandrepository.EnrollmentRepository;
import com.liveklass.enrollment.domain.Enrollment;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.klass.commandrepository.KlassRepository;
import com.liveklass.klass.commandservice.CreateKlassDto;
import com.liveklass.klass.commandservice.KlassCommandService;
import com.liveklass.klass.domain.Klass;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class EnrollmentCommandServiceTest {

	private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final Instant FIXED_INSTANT = Instant.parse("2026-07-12T00:00:00Z");
	private static final LocalDateTime FIXED_NOW = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE_ID);
	private static final Long CREATOR_ID = 1L;
	private static final Long USER_ID = 2L;
	private static final Long OTHER_USER_ID = 3L;

	@Autowired
	private KlassCommandService klassCommandService;

	@Autowired
	private EnrollmentCommandService enrollmentCommandService;

	@Autowired
	private KlassRepository klassRepository;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@BeforeEach
	void setUp() {
		enrollmentRepository.deleteAll();
		klassRepository.deleteAll();
	}

	@Test
	void enrollCreatesPendingEnrollmentAndIncrementsCapacity() {
		Long klassId = createOpenKlass(2);

		Long enrollmentId = enrollmentCommandService.enroll(klassId, USER_ID);

		Enrollment enrollment = enrollmentRepository.getByIdOrThrow(enrollmentId);
		Klass klass = klassRepository.getByIdOrThrow(klassId);
		assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
		assertThat(enrollment.getUserId()).isEqualTo(USER_ID);
		assertThat(klass.getEnrolledCount()).isEqualTo(1);
	}

	@Test
	void enrollRejectsDuplicateActiveEnrollment() {
		Long klassId = createOpenKlass(2);
		enrollmentCommandService.enroll(klassId, USER_ID);

		assertThatThrownBy(() -> enrollmentCommandService.enroll(klassId, USER_ID))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_ACTIVE_ENROLLMENT)
			);
	}

	@Test
	void enrollRejectsWhenCapacityIsFull() {
		Long klassId = createOpenKlass(1);
		enrollmentCommandService.enroll(klassId, USER_ID);

		assertThatThrownBy(() -> enrollmentCommandService.enroll(klassId, OTHER_USER_ID))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CAPACITY_EXCEEDED)
			);
	}

	@Test
	void cancelPendingEnrollmentRestoresCapacity() {
		Long klassId = createOpenKlass(1);
		Long enrollmentId = enrollmentCommandService.enroll(klassId, USER_ID);

		enrollmentCommandService.cancel(enrollmentId, USER_ID);

		Enrollment enrollment = enrollmentRepository.getByIdOrThrow(enrollmentId);
		Klass klass = klassRepository.getByIdOrThrow(klassId);
		assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
		assertThat(enrollment.getCancelledAt()).isEqualTo(FIXED_NOW);
		assertThat(klass.getEnrolledCount()).isZero();
	}

	@Test
	void cancelConfirmedEnrollmentRejectsAfterAvailablePeriod() {
		Long klassId = createOpenKlass(1);
		Long enrollmentId = enrollmentCommandService.enroll(klassId, USER_ID);
		Enrollment enrollment = enrollmentRepository.getByIdOrThrow(enrollmentId);
		enrollment.confirm(FIXED_NOW.minusDays(8));
		enrollmentRepository.saveAndFlush(enrollment);

		assertThatThrownBy(() -> enrollmentCommandService.cancel(enrollmentId, USER_ID))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANCEL_PERIOD_EXPIRED)
			);

		Klass klass = klassRepository.getByIdOrThrow(klassId);
		assertThat(klass.getEnrolledCount()).isEqualTo(1);
	}

	@Test
	void cancelRejectsNonOwner() {
		Long klassId = createOpenKlass(1);
		Long enrollmentId = enrollmentCommandService.enroll(klassId, USER_ID);

		assertThatThrownBy(() -> enrollmentCommandService.cancel(enrollmentId, OTHER_USER_ID))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_ENROLLMENT_ACCESS)
			);
	}

	private Long createOpenKlass(int capacity) {
		LocalDateTime startDate = FIXED_NOW.plusDays(30);
		Long klassId = klassCommandService.create(new CreateKlassDto(
			CREATOR_ID,
			"Enrollment test",
			"Enrollment test description",
			BigDecimal.valueOf(10000),
			capacity,
			startDate,
			startDate.plusDays(30)
		));
		klassCommandService.open(klassId, CREATOR_ID);
		return klassId;
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
