package com.liveklass.klass.readservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.commandrepository.EnrollmentRepository;
import com.liveklass.klass.commandrepository.KlassRepository;
import com.liveklass.klass.commandservice.CreateKlassDto;
import com.liveklass.klass.commandservice.KlassCommandService;
import com.liveklass.klass.domain.enums.KlassStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class KlassReadServiceTest {

	private static final Long CREATOR_ID = 1L;

	@Autowired
	private KlassCommandService klassCommandService;

	@Autowired
	private KlassReadService klassReadService;

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
	void getOneReturnsKlassDetail() {
		Long klassId = klassCommandService.create(createKlassDto("Read one"));
		klassCommandService.open(klassId, CREATOR_ID);

		KlassDto dto = klassReadService.getOne(klassId);

		assertThat(dto.id()).isEqualTo(klassId);
		assertThat(dto.creatorId()).isEqualTo(CREATOR_ID);
		assertThat(dto.title()).isEqualTo("Read one");
		assertThat(dto.status()).isEqualTo(KlassStatus.OPEN);
	}

	@Test
	void getOneRejectsMissingKlass() {
		assertThatThrownBy(() -> klassReadService.getOne(999L))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.KLASS_NOT_FOUND)
			);
	}

	@Test
	void getListReturnsOnlyFilteredStatus() {
		Long openKlassId = klassCommandService.create(createKlassDto("Open klass"));
		klassCommandService.open(openKlassId, CREATOR_ID);
		klassCommandService.create(createKlassDto("Draft klass"));

		Page<KlassDto> page = klassReadService.getList(KlassStatus.OPEN, PageRequest.of(0, 10));

		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent())
			.extracting(KlassDto::id)
			.containsExactly(openKlassId);
	}

	private CreateKlassDto createKlassDto(String title) {
		LocalDateTime startDate = LocalDateTime.of(2026, 8, 1, 0, 0);
		return new CreateKlassDto(
			CREATOR_ID,
			title,
			title + " description",
			BigDecimal.valueOf(10000),
			10,
			startDate,
			startDate.plusDays(30)
		);
	}
}
