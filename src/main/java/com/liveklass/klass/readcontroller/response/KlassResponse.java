package com.liveklass.klass.readcontroller.response;

import com.liveklass.klass.domain.enums.KlassStatus;
import com.liveklass.klass.readservice.KlassDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KlassResponse(
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
	String statusLabel,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static KlassResponse from(KlassDto dto) {
		return new KlassResponse(
			dto.id(),
			dto.creatorId(),
			dto.title(),
			dto.description(),
			dto.price(),
			dto.capacity(),
			dto.enrolledCount(),
			dto.startDate(),
			dto.endDate(),
			dto.status(),
			dto.status().getLabel(),
			dto.createdAt(),
			dto.updatedAt()
		);
	}
}
