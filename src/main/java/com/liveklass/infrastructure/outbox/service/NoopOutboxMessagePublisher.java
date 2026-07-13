package com.liveklass.infrastructure.outbox.service;

import com.liveklass.infrastructure.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class NoopOutboxMessagePublisher implements OutboxMessagePublisher {

	@Override
	public void publish(OutboxEvent outboxEvent) {
		// ë¡œì»¬ êµ¬í˜„?ì„œ???¸ë? ë¸Œë¡œì»??€???±ê³µ ë°œí–‰ë§??œë??ˆì´?˜í•œ??
	}
}
