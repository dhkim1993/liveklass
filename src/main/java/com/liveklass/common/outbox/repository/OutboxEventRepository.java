package com.liveklass.common.outbox.repository;

import com.liveklass.common.outbox.domain.OutboxEvent;
import com.liveklass.common.outbox.domain.enums.OutboxEventStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

	List<OutboxEvent> findTop100ByStatusAndNextRetryAtLessThanEqualOrderByOccurredAtAsc(
		OutboxEventStatus status,
		LocalDateTime nextRetryAt
	);
}
