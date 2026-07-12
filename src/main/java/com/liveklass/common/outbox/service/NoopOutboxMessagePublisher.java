package com.liveklass.common.outbox.service;

import com.liveklass.common.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class NoopOutboxMessagePublisher implements OutboxMessagePublisher {

	@Override
	public void publish(OutboxEvent outboxEvent) {
		// 로컬 구현에서는 외부 브로커 대신 성공 발행만 시뮬레이션한다.
	}
}
