package com.liveklass.enrollment.commandcontroller;

import com.liveklass.enrollment.commandcontroller.response.CreateEnrollmentResponse;
import com.liveklass.enrollment.facade.EnrollmentFacade;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EnrollmentCommandController {

	private static final String USER_ID_HEADER = "X-USER-ID";

	private final EnrollmentFacade enrollmentFacade;

	@PostMapping("/klasses/{klassId}/enrollments")
	public ResponseEntity<CreateEnrollmentResponse> enroll(
		@PathVariable @Positive Long klassId,
		@RequestHeader(USER_ID_HEADER) @Positive Long userId
	) {
		Long enrollmentId = enrollmentFacade.enroll(klassId, userId);

		return ResponseEntity
			.created(URI.create("/api/enrollments/" + enrollmentId))
			.body(new CreateEnrollmentResponse(enrollmentId));
	}

	@PostMapping("/enrollments/{enrollmentId}/cancel")
	public ResponseEntity<Void> cancel(
		@PathVariable @Positive Long enrollmentId,
		@RequestHeader(USER_ID_HEADER) @Positive Long userId
	) {
		enrollmentFacade.cancel(enrollmentId, userId);
		return ResponseEntity.noContent().build();
	}
}
