package com.liveklass.enrollment.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.commandservice.EnrollmentCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

class EnrollmentFacadeTest {

	private final EnrollmentCommandService enrollmentCommandService = mock(EnrollmentCommandService.class);
	private final EnrollmentFacade enrollmentFacade = new EnrollmentFacade(enrollmentCommandService);

	@Test
	void enrollRetriesOptimisticLockFailureAndReturnsResult() {
		when(enrollmentCommandService.enroll(1L, 2L))
			.thenThrow(new OptimisticLockingFailureException("conflict"))
			.thenReturn(10L);

		Long enrollmentId = enrollmentFacade.enroll(1L, 2L);

		assertThat(enrollmentId).isEqualTo(10L);
		verify(enrollmentCommandService, times(2)).enroll(1L, 2L);
	}

	@Test
	void enrollThrowsConcurrencyConflictAfterMaxRetry() {
		when(enrollmentCommandService.enroll(1L, 2L))
			.thenThrow(new OptimisticLockingFailureException("conflict"));

		assertThatThrownBy(() -> enrollmentFacade.enroll(1L, 2L))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONCURRENCY_CONFLICT)
			);

		verify(enrollmentCommandService, times(3)).enroll(1L, 2L);
	}

	@Test
	void cancelRetriesOptimisticLockFailure() {
		doThrow(new OptimisticLockingFailureException("conflict"))
			.doNothing()
			.when(enrollmentCommandService)
			.cancel(1L, 2L);

		enrollmentFacade.cancel(1L, 2L);

		verify(enrollmentCommandService, times(2)).cancel(1L, 2L);
	}
}
