package com.liveklass.infrastructure.outbox.service;

import com.liveklass.infrastructure.outbox.domain.OutboxEvent;

public interface OutboxMessagePublisher {

	void publish(OutboxEvent outboxEvent);
}
