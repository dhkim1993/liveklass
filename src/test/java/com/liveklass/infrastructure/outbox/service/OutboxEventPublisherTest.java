package com.liveklass.infrastructure.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.liveklass.infrastructure.outbox.domain.OutboxEvent;
import com.liveklass.infrastructure.outbox.domain.enums.OutboxEventStatus;
import com.liveklass.infrastructure.outbox.domain.enums.OutboxEventType;
import com.liveklass.infrastructure.outbox.repository.OutboxEventRepository;
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
class OutboxEventPublisherTest {

	private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final Instant FIXED_INSTANT = Instant.parse("2026-07-12T00:00:00Z");
	private static final LocalDateTime FIXED_NOW = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE_ID);

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private OutboxEventPublisher outboxEventPublisher;

	@BeforeEach
	void setUp() {
		outboxEventRepository.deleteAll();
	}

	@Test
	void publishPendingMarksEventAsPublished() {
		OutboxEvent outboxEvent = OutboxEvent.create(
			"Enrollment",
			1L,
			OutboxEventType.ENROLLMENT_CREATED,
			"{\"enrollmentId\":1}",
			FIXED_NOW
		);
		Long outboxEventId = outboxEventRepository.save(outboxEvent).getId();

		int publishedCount = outboxEventPublisher.publishPending();

		OutboxEvent publishedEvent = outboxEventRepository.findById(outboxEventId).orElseThrow();
		assertThat(publishedCount).isEqualTo(1);
		assertThat(publishedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
		assertThat(publishedEvent.getPublishedAt()).isEqualTo(FIXED_NOW);
	}

	@Test
	void publishPendingSkipsFutureRetryEvent() {
		OutboxEvent outboxEvent = OutboxEvent.create(
			"Enrollment",
			1L,
			OutboxEventType.ENROLLMENT_CREATED,
			"{\"enrollmentId\":1}",
			FIXED_NOW.plusMinutes(1)
		);
		outboxEventRepository.save(outboxEvent);

		int publishedCount = outboxEventPublisher.publishPending();

		assertThat(publishedCount).isZero();
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
