package com.liveklass.infrastructure.outbox.service;

import com.liveklass.infrastructure.outbox.domain.OutboxEvent;
import com.liveklass.infrastructure.outbox.domain.enums.OutboxEventStatus;
import com.liveklass.infrastructure.outbox.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

	private static final int MAX_RETRY_COUNT = 3;
	private static final long RETRY_DELAY_SECONDS = 60;

	private final OutboxEventRepository outboxEventRepository;
	private final OutboxMessagePublisher outboxMessagePublisher;
	private final Clock clock;

	@Transactional
	public int publishPending() {
		LocalDateTime now = LocalDateTime.now(clock);
		List<OutboxEvent> outboxEvents =
			outboxEventRepository.findTop100ByStatusAndNextRetryAtLessThanEqualOrderByOccurredAtAsc(
				OutboxEventStatus.PENDING,
				now
			);

		outboxEvents.forEach(outboxEvent -> publish(outboxEvent, now));
		return outboxEvents.size();
	}

	private void publish(OutboxEvent outboxEvent, LocalDateTime now) {
		try {
			outboxMessagePublisher.publish(outboxEvent);
			outboxEvent.markPublished(now);
		} catch (RuntimeException exception) {
			outboxEvent.markPublishFailed(now, MAX_RETRY_COUNT, RETRY_DELAY_SECONDS);
		}
	}
}
