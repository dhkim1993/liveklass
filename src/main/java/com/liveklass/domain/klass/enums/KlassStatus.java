package com.liveklass.domain.klass.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KlassStatus {
	DRAFT("초안"),
	OPEN("모집 중"),
	CLOSED("모집 마감");

	public static final Map<String, String> FORM_OPTIONS = Collections.unmodifiableMap(
		Arrays.stream(values())
			.collect(Collectors.toMap(
				KlassStatus::name,
				KlassStatus::getLabel,
				(left, right) -> left,
				LinkedHashMap::new
			))
	);

	private final String label;
}
