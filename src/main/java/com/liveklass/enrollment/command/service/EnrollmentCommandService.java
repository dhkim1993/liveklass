package com.liveklass.enrollment.command.service;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.infrastructure.outbox.domain.enums.OutboxEventType;
import com.liveklass.infrastructure.outbox.service.OutboxEventService;
import com.liveklass.enrollment.command.repository.EnrollmentRepository;
import com.liveklass.enrollment.domain.Enrollment;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.klass.command.repository.KlassRepository;
import com.liveklass.klass.domain.Klass;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentCommandService {

	private final KlassRepository klassRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final OutboxEventService outboxEventService;
	private final Clock clock;

	public Long enroll(Long klassId, Long userId) {
		Klass klass = klassRepository.getByIdOrThrow(klassId);
		klass.validateEnrollable();

		if (enrollmentRepository.existsByKlassIdAndUserIdAndStatusIn(
			klassId,
			userId,
			EnrollmentStatus.ACTIVE_STATUSES
		)) {
			throw new LiveKlassException(ErrorCode.DUPLICATE_ACTIVE_ENROLLMENT);
		}

		klass.incrementCapacity();
		Enrollment enrollment = Enrollment.pending(klass, userId);
		Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

		outboxEventService.save(
			"Enrollment",
			savedEnrollment.getId(),
			OutboxEventType.ENROLLMENT_CREATED,
			toEnrollmentPayload(savedEnrollment)
		);

		return savedEnrollment.getId();
	}

	public void cancel(Long enrollmentId, Long requesterId) {
		Enrollment enrollment = enrollmentRepository.getByIdOrThrow(enrollmentId);
		enrollment.validateOwner(requesterId);

		LocalDateTime now = LocalDateTime.now(clock);

		boolean cancelled = enrollment.cancel(now);
		if (cancelled) {
			enrollment.getKlass().decrementCapacity();
			outboxEventService.save(
				"Enrollment",
				enrollment.getId(),
				OutboxEventType.ENROLLMENT_CANCELLED,
				toEnrollmentPayload(enrollment)
			);
		}
	}

	private Map<String, Object> toEnrollmentPayload(Enrollment enrollment) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("enrollmentId", enrollment.getId());
		payload.put("klassId", enrollment.getKlass().getId());
		payload.put("userId", enrollment.getUserId());
		payload.put("status", enrollment.getStatus().name());
		return payload;
	}
}
