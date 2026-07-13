package com.liveklass.enrollment.read.service;

import com.liveklass.enrollment.domain.Enrollment;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import java.time.LocalDateTime;

public record EnrollmentDto(
	Long id,
	Long klassId,
	String klassTitle,
	Long userId,
	EnrollmentStatus status,
	LocalDateTime confirmedAt,
	LocalDateTime cancelledAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static EnrollmentDto from(Enrollment enrollment) {
		return new EnrollmentDto(
			enrollment.getId(),
			enrollment.getKlass().getId(),
			enrollment.getKlass().getTitle(),
			enrollment.getUserId(),
			enrollment.getStatus(),
			enrollment.getConfirmedAt(),
			enrollment.getCancelledAt(),
			enrollment.getCreatedAt(),
			enrollment.getUpdatedAt()
		);
	}
}
