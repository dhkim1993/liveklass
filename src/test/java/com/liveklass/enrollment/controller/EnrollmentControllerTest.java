package com.liveklass.enrollment.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveklass.enrollment.command.repository.EnrollmentRepository;
import com.liveklass.klass.command.controller.request.CreateKlassRequest;
import com.liveklass.klass.command.repository.KlassRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentControllerTest {

	private static final String USER_ID_HEADER = "X-USER-ID";
	private static final Long CREATOR_ID = 1L;
	private static final Long USER_ID = 2L;
	private static final Long OTHER_USER_ID = 3L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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
	void enrollCreatesEnrollment() throws Exception {
		Long klassId = createOpenKlass("Enrollment API", 10);

		mockMvc.perform(post("/api/klasses/{klassId}/enrollments", klassId)
				.header(USER_ID_HEADER, USER_ID))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	void cancelEnrollment() throws Exception {
		Long klassId = createOpenKlass("Cancel API", 10);
		Long enrollmentId = createEnrollment(klassId, USER_ID);

		mockMvc.perform(post("/api/enrollments/{enrollmentId}/cancel", enrollmentId)
				.header(USER_ID_HEADER, USER_ID))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/users/{userId}/enrollments", USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].status").value("CANCELLED"));
	}

	@Test
	void getMyListReturnsOnlyUserEnrollments() throws Exception {
		Long klassId = createOpenKlass("My list API", 10);
		Long enrollmentId = createEnrollment(klassId, USER_ID);
		createEnrollment(klassId, OTHER_USER_ID);

		mockMvc.perform(get("/api/users/{userId}/enrollments", USER_ID)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].id").value(enrollmentId))
			.andExpect(jsonPath("$.content[0].klassTitle").value("My list API"))
			.andExpect(jsonPath("$.content[0].statusLabel").value("ê²°ì œ ?€ê¸?));
	}

	@Test
	void getListByKlassIdRequiresCreator() throws Exception {
		Long klassId = createOpenKlass("Creator list API", 10);
		Long firstEnrollmentId = createEnrollment(klassId, USER_ID);
		Long secondEnrollmentId = createEnrollment(klassId, OTHER_USER_ID);

		mockMvc.perform(get("/api/klasses/{klassId}/enrollments", klassId)
				.header(USER_ID_HEADER, CREATOR_ID)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].id").value(secondEnrollmentId))
			.andExpect(jsonPath("$.content[1].id").value(firstEnrollmentId));

		mockMvc.perform(get("/api/klasses/{klassId}/enrollments", klassId)
				.header(USER_ID_HEADER, OTHER_USER_ID))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN_KLASS_ACCESS"));
	}

	@Test
	void getFormReturnsEnrollmentStatusOptions() throws Exception {
		mockMvc.perform(get("/api/enrollments/form"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.PENDING").value("ê²°ì œ ?€ê¸?))
			.andExpect(jsonPath("$.CONFIRMED").value("?˜ê°• ?•ì •"))
			.andExpect(jsonPath("$.CANCELLED").value("ì·¨ì†Œ??));
	}

	private Long createOpenKlass(String title, int capacity) throws Exception {
		Long klassId = createKlass(title, capacity);

		mockMvc.perform(patch("/api/klasses/{klassId}/open", klassId)
				.header(USER_ID_HEADER, CREATOR_ID))
			.andExpect(status().isNoContent());

		return klassId;
	}

	private Long createKlass(String title, int capacity) throws Exception {
		LocalDateTime startDate = LocalDateTime.of(2026, 8, 1, 0, 0);
		CreateKlassRequest request = new CreateKlassRequest(
			title,
			title + " description",
			BigDecimal.valueOf(10000),
			capacity,
			startDate,
			startDate.plusDays(30)
		);

		String response = mockMvc.perform(post("/api/klasses")
				.header(USER_ID_HEADER, CREATOR_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return objectMapper.readTree(response).get("id").asLong();
	}

	private Long createEnrollment(Long klassId, Long userId) throws Exception {
		String response = mockMvc.perform(post("/api/klasses/{klassId}/enrollments", klassId)
				.header(USER_ID_HEADER, userId))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return objectMapper.readTree(response).get("id").asLong();
	}
}
