package com.liveklass.klass.readservice;

import com.liveklass.klass.domain.Klass;
import com.liveklass.klass.domain.enums.KlassStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KlassDto(
	Long id,
	Long creatorId,
	String title,
	String description,
	BigDecimal price,
	int capacity,
	int enrolledCount,
	LocalDateTime startDate,
	LocalDateTime endDate,
	KlassStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static KlassDto from(Klass klass) {
		return new KlassDto(
			klass.getId(),
			klass.getCreatorId(),
			klass.getTitle(),
			klass.getDescription(),
			klass.getPrice(),
			klass.getCapacity(),
			klass.getEnrolledCount(),
			klass.getStartDate(),
			klass.getEndDate(),
			klass.getStatus(),
			klass.getCreatedAt(),
			klass.getUpdatedAt()
		);
	}
}
