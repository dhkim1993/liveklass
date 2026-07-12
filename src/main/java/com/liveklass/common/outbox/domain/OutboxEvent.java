package com.liveklass.common.outbox.domain;

import com.liveklass.common.outbox.domain.enums.OutboxEventStatus;
import com.liveklass.common.outbox.domain.enums.OutboxEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "outbox_events",
	indexes = {
		@Index(name = "idx_outbox_events_status_next_retry", columnList = "status,next_retry_at"),
		@Index(name = "idx_outbox_events_aggregate", columnList = "aggregate_type,aggregate_id")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "aggregate_type", nullable = false, length = 50)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private Long aggregateId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 50)
	private OutboxEventType eventType;

	@Lob
	@Column(nullable = false)
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OutboxEventStatus status;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "next_retry_at", nullable = false)
	private LocalDateTime nextRetryAt;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	private OutboxEvent(
		String aggregateType,
		Long aggregateId,
		OutboxEventType eventType,
		String payload,
		LocalDateTime occurredAt
	) {
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
		this.status = OutboxEventStatus.PENDING;
		this.retryCount = 0;
		this.nextRetryAt = occurredAt;
		this.occurredAt = occurredAt;
	}

	public static OutboxEvent create(
		String aggregateType,
		Long aggregateId,
		OutboxEventType eventType,
		String payload,
		LocalDateTime occurredAt
	) {
		return new OutboxEvent(aggregateType, aggregateId, eventType, payload, occurredAt);
	}

	public void markPublished(LocalDateTime publishedAt) {
		this.status = OutboxEventStatus.PUBLISHED;
		this.publishedAt = publishedAt;
	}

	public void markPublishFailed(LocalDateTime now, int maxRetryCount, long retryDelaySeconds) {
		retryCount++;
		if (retryCount >= maxRetryCount) {
			status = OutboxEventStatus.FAILED;
			nextRetryAt = now;
			return;
		}
		nextRetryAt = now.plusSeconds(retryDelaySeconds);
	}
}
