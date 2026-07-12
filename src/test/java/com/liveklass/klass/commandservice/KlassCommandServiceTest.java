package com.liveklass.klass.commandservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.commandrepository.EnrollmentRepository;
import com.liveklass.klass.commandrepository.KlassRepository;
import com.liveklass.klass.domain.Klass;
import com.liveklass.klass.domain.enums.KlassStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KlassCommandServiceTest {

	private static final Long CREATOR_ID = 1L;
	private static final Long OTHER_USER_ID = 2L;

	@Autowired
	private KlassCommandService klassCommandService;

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
	void createSavesDraftKlass() {
		Long klassId = klassCommandService.create(createKlassDto("Spring Boot", 10));

		Klass klass = klassRepository.getByIdOrThrow(klassId);

		assertThat(klass.getCreatorId()).isEqualTo(CREATOR_ID);
		assertThat(klass.getTitle()).isEqualTo("Spring Boot");
		assertThat(klass.getStatus()).isEqualTo(KlassStatus.DRAFT);
		assertThat(klass.getEnrolledCount()).isZero();
	}

	@Test
	void openChangesDraftToOpen() {
		Long klassId = klassCommandService.create(createKlassDto("JPA", 10));

		klassCommandService.open(klassId, CREATOR_ID);

		Klass klass = klassRepository.getByIdOrThrow(klassId);
		assertThat(klass.getStatus()).isEqualTo(KlassStatus.OPEN);
	}

	@Test
	void closeChangesOpenToClosed() {
		Long klassId = klassCommandService.create(createKlassDto("QueryDSL", 10));
		klassCommandService.open(klassId, CREATOR_ID);

		klassCommandService.close(klassId, CREATOR_ID);

		Klass klass = klassRepository.getByIdOrThrow(klassId);
		assertThat(klass.getStatus()).isEqualTo(KlassStatus.CLOSED);
	}

	@Test
	void openRejectsNonCreator() {
		Long klassId = klassCommandService.create(createKlassDto("Security", 10));

		assertThatThrownBy(() -> klassCommandService.open(klassId, OTHER_USER_ID))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_KLASS_ACCESS)
			);
	}

	@Test
	void closeRejectsDraftKlass() {
		Long klassId = klassCommandService.create(createKlassDto("Draft", 10));

		assertThatThrownBy(() -> klassCommandService.close(klassId, CREATOR_ID))
			.isInstanceOfSatisfying(LiveKlassException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.KLASS_CANNOT_CLOSE)
			);
	}

	private CreateKlassDto createKlassDto(String title, int capacity) {
		LocalDateTime startDate = LocalDateTime.of(2026, 8, 1, 0, 0);
		return new CreateKlassDto(
			CREATOR_ID,
			title,
			title + " description",
			BigDecimal.valueOf(10000),
			capacity,
			startDate,
			startDate.plusDays(30)
		);
	}
}
