package com.liveklass.enrollment.read.controller.response;

import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.enrollment.read.service.EnrollmentDto;
import java.time.LocalDateTime;

public record EnrollmentResponse(
	Long id,
	Long klassId,
	String klassTitle,
	Long userId,
	EnrollmentStatus status,
	String statusLabel,
	LocalDateTime confirmedAt,
	LocalDateTime cancelledAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static EnrollmentResponse from(EnrollmentDto dto) {
		return new EnrollmentResponse(
			dto.id(),
			dto.klassId(),
			dto.klassTitle(),
			dto.userId(),
			dto.status(),
			dto.status().getLabel(),
			dto.confirmedAt(),
			dto.cancelledAt(),
			dto.createdAt(),
			dto.updatedAt()
		);
	}
}
