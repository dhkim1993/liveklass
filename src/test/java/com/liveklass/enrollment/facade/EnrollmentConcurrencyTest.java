package com.liveklass.enrollment.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.command.repository.EnrollmentRepository;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.infrastructure.outbox.repository.OutboxEventRepository;
import com.liveklass.klass.command.repository.KlassRepository;
import com.liveklass.klass.command.service.CreateKlassDto;
import com.liveklass.klass.command.service.KlassCommandService;
import com.liveklass.klass.domain.Klass;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class EnrollmentConcurrencyTest {

	private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final Instant FIXED_INSTANT = Instant.parse("2026-07-12T00:00:00Z");
	private static final LocalDateTime FIXED_NOW = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE_ID);
	private static final Long CREATOR_ID = 1L;
	private static final int CAPACITY = 1;
	private static final int REQUEST_COUNT = 8;

	@Autowired
	private KlassCommandService klassCommandService;

	@Autowired
	private EnrollmentFacade enrollmentFacade;

	@Autowired
	private KlassRepository klassRepository;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@BeforeEach
	void setUp() {
		outboxEventRepository.deleteAll();
		enrollmentRepository.deleteAll();
		klassRepository.deleteAll();
	}

	@Test
	void concurrentEnrollmentsDoNotExceedCapacity() throws InterruptedException {
		Long klassId = createOpenKlass(CAPACITY);
		ExecutorService executorService = Executors.newFixedThreadPool(REQUEST_COUNT);
		CountDownLatch readyLatch = new CountDownLatch(REQUEST_COUNT);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(REQUEST_COUNT);
		AtomicInteger successCount = new AtomicInteger();
		AtomicReference<Throwable> unexpectedException = new AtomicReference<>();
		Set<ErrorCode> failureErrorCodes = Collections.synchronizedSet(EnumSet.noneOf(ErrorCode.class));

		for (int index = 0; index < REQUEST_COUNT; index++) {
			long userId = 100L + index;
			executorService.submit(() -> {
				readyLatch.countDown();
				try {
					startLatch.await();
					enrollmentFacade.enroll(klassId, userId);
					successCount.incrementAndGet();
				} catch (LiveKlassException exception) {
					failureErrorCodes.add(exception.getErrorCode());
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					unexpectedException.compareAndSet(null, exception);
				} catch (Throwable throwable) {
					unexpectedException.compareAndSet(null, throwable);
				} finally {
					doneLatch.countDown();
				}
			});
		}

		assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
		startLatch.countDown();
		assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
		executorService.shutdown();
		assertThat(unexpectedException.get()).isNull();

		Klass klass = klassRepository.getByIdOrThrow(klassId);
		long activeEnrollmentCount = enrollmentRepository.countByKlassIdAndStatusIn(
			klassId,
			EnrollmentStatus.ACTIVE_STATUSES
		);

		assertThat(successCount.get()).isEqualTo(CAPACITY);
		assertThat(activeEnrollmentCount).isEqualTo(CAPACITY);
		assertThat(klass.getEnrolledCount()).isEqualTo(CAPACITY);
		assertThat(failureErrorCodes)
			.isNotEmpty()
			.allSatisfy(errorCode ->
				assertThat(errorCode).isIn(ErrorCode.CAPACITY_EXCEEDED, ErrorCode.CONCURRENCY_CONFLICT)
			);
	}

	private Long createOpenKlass(int capacity) {
		LocalDateTime startDate = FIXED_NOW.plusDays(30);
		Long klassId = klassCommandService.create(new CreateKlassDto(
			CREATOR_ID,
			"Concurrent Enrollment test",
			"Concurrent Enrollment test description",
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
