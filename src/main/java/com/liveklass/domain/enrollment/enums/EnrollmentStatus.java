package com.liveklass.domain.enrollment.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnrollmentStatus {
	PENDING("결제 대기"),
	CONFIRMED("수강 확정"),
	CANCELLED("취소됨");

	public static final Map<String, String> FORM_OPTIONS = Collections.unmodifiableMap(
		Arrays.stream(values())
			.collect(Collectors.toMap(
				EnrollmentStatus::name,
				EnrollmentStatus::getLabel,
				(left, right) -> left,
				LinkedHashMap::new
			))
	);

	private final String label;
}
