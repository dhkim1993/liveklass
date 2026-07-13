package com.liveklass.klass.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class KlassControllerTest {

	private static final String USER_ID_HEADER = "X-USER-ID";
	private static final Long CREATOR_ID = 1L;

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
	void createReturnsCreatedResponse() throws Exception {
		mockMvc.perform(post("/api/klasses")
				.header(USER_ID_HEADER, CREATOR_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest("Spring Boot"))))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/klasses/")))
			.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	void createRejectsInvalidRequest() throws Exception {
		CreateKlassRequest request = new CreateKlassRequest(
			"",
			"description",
			BigDecimal.valueOf(-1),
			0,
			LocalDateTime.of(2026, 8, 1, 0, 0),
			LocalDateTime.of(2026, 7, 1, 0, 0)
		);

		mockMvc.perform(post("/api/klasses")
				.header(USER_ID_HEADER, CREATOR_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void openAndCloseKlass() throws Exception {
		Long klassId = createKlass("Lifecycle");

		mockMvc.perform(patch("/api/klasses/{klassId}/open", klassId)
				.header(USER_ID_HEADER, CREATOR_ID))
			.andExpect(status().isNoContent());

		mockMvc.perform(patch("/api/klasses/{klassId}/close", klassId)
				.header(USER_ID_HEADER, CREATOR_ID))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/klasses/{klassId}", klassId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(klassId))
			.andExpect(jsonPath("$.status").value("CLOSED"))
			.andExpect(jsonPath("$.statusLabel").value("모집 마감"));
	}

	@Test
	void getListFiltersByStatus() throws Exception {
		Long openKlassId = createKlass("Open klass");
		createKlass("Draft klass");
		mockMvc.perform(patch("/api/klasses/{klassId}/open", openKlassId)
				.header(USER_ID_HEADER, CREATOR_ID))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/klasses")
				.param("status", "OPEN")
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].id").value(openKlassId))
			.andExpect(jsonPath("$.content[0].status").value("OPEN"));
	}

	@Test
	void getFormReturnsKlassStatusOptions() throws Exception {
		mockMvc.perform(get("/api/klasses/form"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.DRAFT").value("초안"))
			.andExpect(jsonPath("$.OPEN").value("모집 중"))
			.andExpect(jsonPath("$.CLOSED").value("모집 마감"));
	}

	private Long createKlass(String title) throws Exception {
		String response = mockMvc.perform(post("/api/klasses")
				.header(USER_ID_HEADER, CREATOR_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest(title))))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return objectMapper.readTree(response).get("id").asLong();
	}

	private CreateKlassRequest createRequest(String title) {
		LocalDateTime startDate = LocalDateTime.of(2026, 8, 1, 0, 0);
		return new CreateKlassRequest(
			title,
			title + " description",
			BigDecimal.valueOf(10000),
			10,
			startDate,
			startDate.plusDays(30)
		);
	}
}
