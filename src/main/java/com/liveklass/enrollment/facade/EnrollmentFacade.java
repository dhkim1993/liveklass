package com.liveklass.enrollment.facade;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.command.service.EnrollmentCommandService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentFacade {

	private static final int MAX_RETRY_COUNT = 3;

	private final EnrollmentCommandService enrollmentCommandService;

	public Long enroll(Long klassId, Long userId) {
		return executeWithOptimisticLockRetry(() -> enrollmentCommandService.enroll(klassId, userId));
	}

	public void cancel(Long enrollmentId, Long userId) {
		executeWithOptimisticLockRetry(() -> {
			enrollmentCommandService.cancel(enrollmentId, userId);
			return null;
		});
	}

	private <T> T executeWithOptimisticLockRetry(EnrollmentCommandSupplier<T> supplier) {
		for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
			try {
				return supplier.get();
			} catch (OptimisticLockingFailureException | OptimisticLockException exception) {
				if (attempt == MAX_RETRY_COUNT) {
					throw new LiveKlassException(ErrorCode.CONCURRENCY_CONFLICT);
				}
			}
		}
		throw new LiveKlassException(ErrorCode.CONCURRENCY_CONFLICT);
	}

	@FunctionalInterface
	private interface EnrollmentCommandSupplier<T> {
		T get();
	}
}
