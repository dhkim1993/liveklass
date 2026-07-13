package com.liveklass.klass.command.controller;

import com.liveklass.klass.command.controller.request.CreateKlassRequest;
import com.liveklass.klass.command.controller.response.CreateKlassResponse;
import com.liveklass.klass.command.service.KlassCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/klasses")
public class KlassCommandController {

	private static final String USER_ID_HEADER = "X-USER-ID";

	private final KlassCommandService klassCommandService;

	@PostMapping
	public ResponseEntity<CreateKlassResponse> create(
		@RequestHeader(USER_ID_HEADER) @Positive Long userId,
		@Valid @RequestBody CreateKlassRequest request
	) {
		Long klassId = klassCommandService.create(request.toDto(userId));

		return ResponseEntity
			.created(URI.create("/api/klasses/" + klassId))
			.body(new CreateKlassResponse(klassId));
	}

	@PatchMapping("/{klassId}/open")
	public ResponseEntity<Void> open(
		@PathVariable @Positive Long klassId,
		@RequestHeader(USER_ID_HEADER) @Positive Long userId
	) {
		klassCommandService.open(klassId, userId);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{klassId}/close")
	public ResponseEntity<Void> close(
		@PathVariable @Positive Long klassId,
		@RequestHeader(USER_ID_HEADER) @Positive Long userId
	) {
		klassCommandService.close(klassId, userId);
		return ResponseEntity.noContent().build();
	}
}
