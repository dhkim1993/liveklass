package com.liveklass.enrollment.commandservice;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.commandrepository.EnrollmentRepository;
import com.liveklass.enrollment.domain.Enrollment;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.klass.commandrepository.KlassRepository;
import com.liveklass.klass.domain.Klass;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentCommandService {

	private final KlassRepository klassRepository;
	private final EnrollmentRepository enrollmentRepository;
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

		return enrollmentRepository.save(enrollment).getId();
	}

	public void cancel(Long enrollmentId, Long requesterId) {
		Enrollment enrollment = enrollmentRepository.getByIdOrThrow(enrollmentId);
		enrollment.validateOwner(requesterId);

		LocalDateTime now = LocalDateTime.now(clock);

		boolean cancelled = enrollment.cancel(now);
		if (cancelled) {
			enrollment.getKlass().decrementCapacity();
		}
	}
}
