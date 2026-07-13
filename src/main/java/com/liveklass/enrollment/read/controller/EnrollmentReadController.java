package com.liveklass.enrollment.read.controller;

import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.enrollment.read.controller.response.EnrollmentResponse;
import com.liveklass.enrollment.read.service.EnrollmentReadService;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EnrollmentReadController {

	private static final String USER_ID_HEADER = "X-USER-ID";

	private final EnrollmentReadService enrollmentReadService;

	@GetMapping("/users/{userId}/enrollments")
	public ResponseEntity<Page<EnrollmentResponse>> getMyList(
		@PathVariable @Positive Long userId,
		Pageable pageable
	) {
		return ResponseEntity.ok(enrollmentReadService.getMyList(userId, pageable).map(EnrollmentResponse::from));
	}

	@GetMapping("/klasses/{klassId}/enrollments")
	public ResponseEntity<Page<EnrollmentResponse>> getListByKlassId(
		@PathVariable @Positive Long klassId,
		@RequestHeader(USER_ID_HEADER) @Positive Long userId,
		Pageable pageable
	) {
		return ResponseEntity.ok(
			enrollmentReadService.getListByKlassId(klassId, userId, pageable).map(EnrollmentResponse::from)
		);
	}

	@GetMapping("/enrollments/form")
	public ResponseEntity<Map<String, String>> getForm() {
		return ResponseEntity.ok(EnrollmentStatus.FORM_OPTIONS);
	}
}
