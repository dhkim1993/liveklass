package com.liveklass.infrastructure.outbox.service;

import com.liveklass.infrastructure.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class NoopOutboxMessagePublisher implements OutboxMessagePublisher {

	@Override
	public void publish(OutboxEvent outboxEvent) {
	}
}
