package com.liveklass.infrastructure.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveklass.infrastructure.outbox.domain.OutboxEvent;
import com.liveklass.infrastructure.outbox.domain.enums.OutboxEventType;
import com.liveklass.infrastructure.outbox.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public void save(
		String aggregateType,
		Long aggregateId,
		OutboxEventType eventType,
		Object payload
	) {
		OutboxEvent outboxEvent = OutboxEvent.create(
			aggregateType,
			aggregateId,
			eventType,
			toJson(payload),
			LocalDateTime.now(clock)
		);

		outboxEventRepository.save(outboxEvent);
	}

	private String toJson(Object payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Cannot serialize outbox payload to JSON.", exception);
		}
	}
}
