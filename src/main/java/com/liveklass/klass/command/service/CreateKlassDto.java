package com.liveklass.klass.command.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateKlassDto(
	Long creatorId,
	String title,
	String description,
	BigDecimal price,
	int capacity,
	LocalDateTime startDate,
	LocalDateTime endDate
) {
}
