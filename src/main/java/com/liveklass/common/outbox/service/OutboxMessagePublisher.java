package com.liveklass.common.outbox.service;

import com.liveklass.common.outbox.domain.OutboxEvent;

public interface OutboxMessagePublisher {

	void publish(OutboxEvent outboxEvent);
}
