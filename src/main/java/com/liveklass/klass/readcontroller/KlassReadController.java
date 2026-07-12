package com.liveklass.klass.readcontroller;

import com.liveklass.klass.domain.enums.KlassStatus;
import com.liveklass.klass.readcontroller.response.KlassResponse;
import com.liveklass.klass.readservice.KlassReadService;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/klasses")
public class KlassReadController {

	private final KlassReadService klassReadService;

	@GetMapping
	public ResponseEntity<Page<KlassResponse>> getList(
		@RequestParam(required = false) KlassStatus status,
		Pageable pageable
	) {
		return ResponseEntity.ok(klassReadService.getList(status, pageable).map(KlassResponse::from));
	}

	@GetMapping("/{klassId}")
	public ResponseEntity<KlassResponse> getOne(@PathVariable @Positive Long klassId) {
		return ResponseEntity.ok(KlassResponse.from(klassReadService.getOne(klassId)));
	}

	@GetMapping("/form")
	public ResponseEntity<Map<String, String>> getForm() {
		return ResponseEntity.ok(KlassStatus.FORM_OPTIONS);
	}
}
