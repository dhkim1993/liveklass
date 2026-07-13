package com.liveklass.enrollment.read.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.command.repository.EnrollmentRepository;
import com.liveklass.enrollment.command.service.EnrollmentCommandService;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.klass.command.repository.KlassRepository;
import com.liveklass.klass.command.service.CreateKlassDto;
import com.liveklass.klass.command.service.KlassCommandService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class EnrollmentReadServiceTest {

	private static final Long CREATOR_ID = 1L;
	private static final Long USER_ID = 2L;
	private static final Long OTHER_USER_ID = 3L;

	@Autowired
	private KlassCommandService klassCommandService;

	@Autowired
	private EnrollmentCommandService enrollmentCommandService;

	@Autowired
	private EnrollmentReadService enrollmentReadService;

	@Autowired
	private KlassRepository klassRepository;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@BeforeEach
	void setUp() {
		enrollmentRepository.deleteAll();
		klassRepository.deleteAll();
	}

	@Test
	void getMyListReturnsUserEnrollments() {
		Long klassId = createOpenKlass("My enrollment", 10);
		Long enrollmentId = enrollmentCommandService.enroll(klassId, USER_ID);
		enrollmentCommandService.enroll(klassId, OTHER_USER_ID);

		Page<EnrollmentDto> page = enrollmentReadService.getMyList(USER_ID, PageRequest.of(0, 10));

		assertThat(page.getTotalElements()).isEqualTo(1);
		EnrollmentDto dto = page.getContent().getFirst();
		assertThat(dto.id()).isEqualTo(enrollmentId);
		assertThat(dto.klassId()).isEqualTo(klassId);
		assertThat(dto.klassTitle()).isEqualTo("My enrollment");
		assertThat(dto.status()).isEqualTo(EnrollmentStatus.PENDING);
	}

	@Test
	void getListByKlassIdReturnsKlassEnrollmentsForCreator() {
		Long klassId = createOpenKlass("Creator enrollment", 10);
		Long firstEnrollmentId = enrollmentCommandService.enroll(klassId, USER_ID);
		Long secondEnrollmentId = enrollmentCommandService.enroll(klassId, OTHER_USER_ID);

		Page<EnrollmentDto> page = enrollmentReadService.getListByKlassId(
			klassId,
			CREATOR_ID,
			PageRequest.of(0, 10)
		);

		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent())
			.extracting(EnrollmentDto::id)
			.containsExactly(secondEnrollmentId, firstEnrollmentId);
	}

	@Test
	void getListByKlassIdRejectsNonCreator() {
		Long klassId = createOpenKlass("Forbidden enrollment", 10);

		assertThatThrownBy(() -> enrollmentReadService.getListByKlassId(
			klassId,
			OTHER_USER_ID,
			PageRequest.of(0, 10)
		))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_KLASS_ACCESS)
			);
	}

	private Long createOpenKlass(String title, int capacity) {
		LocalDateTime startDate = LocalDateTime.of(2026, 8, 1, 0, 0);
		Long klassId = klassCommandService.create(new CreateKlassDto(
			CREATOR_ID,
			title,
			title + " description",
			BigDecimal.valueOf(10000),
			capacity,
			startDate,
			startDate.plusDays(30)
		));
		klassCommandService.open(klassId, CREATOR_ID);
		return klassId;
	}
}
