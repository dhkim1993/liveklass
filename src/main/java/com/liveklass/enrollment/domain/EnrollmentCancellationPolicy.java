package com.liveklass.enrollment.domain;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import java.time.LocalDateTime;

final class EnrollmentCancellationPolicy {

	private static final int AVAILABLE_DAYS_AFTER_CONFIRM = 7;

	private EnrollmentCancellationPolicy() {
	}

	static void validateConfirmedCancellation(
		LocalDateTime confirmedAt,
		LocalDateTime klassStartDate,
		LocalDateTime now
	) {
		if (now.isAfter(confirmedAt.plusDays(AVAILABLE_DAYS_AFTER_CONFIRM))) {
			throw new LiveKlassException(ErrorCode.CANCEL_PERIOD_EXPIRED);
		}
		if (!now.isBefore(klassStartDate)) {
			throw new LiveKlassException(ErrorCode.CANCEL_PERIOD_EXPIRED);
		}
	}
}
