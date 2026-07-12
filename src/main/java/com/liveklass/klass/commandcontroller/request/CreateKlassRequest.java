package com.liveklass.klass.commandcontroller.request;

import com.liveklass.klass.commandservice.CreateKlassDto;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateKlassRequest(
	@NotBlank
	@Size(max = 100)
	String title,

	@NotBlank
	@Size(max = 1000)
	String description,

	@NotNull
	@PositiveOrZero
	BigDecimal price,

	@NotNull
	@Positive
	Integer capacity,

	@NotNull
	LocalDateTime startDate,

	@NotNull
	LocalDateTime endDate
) {

	public CreateKlassDto toDto(Long creatorId) {
		return new CreateKlassDto(
			creatorId,
			title,
			description,
			price,
			capacity,
			startDate,
			endDate
		);
	}

	@AssertTrue(message = "endDate는 startDate 이후여야 합니다.")
	public boolean isValidPeriod() {
		return startDate == null || endDate == null || endDate.isAfter(startDate);
	}
}
