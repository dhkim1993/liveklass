package com.liveklass.payment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveklass.infrastructure.outbox.repository.OutboxEventRepository;
import com.liveklass.enrollment.command.repository.EnrollmentRepository;
import com.liveklass.klass.command.controller.request.CreateKlassRequest;
import com.liveklass.klass.command.repository.KlassRepository;
import com.liveklass.payment.command.controller.request.PaymentCallbackRequest;
import com.liveklass.payment.command.repository.PaymentIdempotencyRepository;
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
class PaymentCallbackControllerTest {

	private static final String USER_ID_HEADER = "X-USER-ID";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final Long CREATOR_ID = 1L;
	private static final Long USER_ID = 2L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private KlassRepository klassRepository;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@Autowired
	private PaymentIdempotencyRepository paymentIdempotencyRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@BeforeEach
	void setUp() {
		outboxEventRepository.deleteAll();
		paymentIdempotencyRepository.deleteAll();
		enrollmentRepository.deleteAll();
		klassRepository.deleteAll();
	}

	@Test
	void callbackConfirmsEnrollment() throws Exception {
		Long klassId = createOpenKlass("Payment callback API");
		Long enrollmentId = createEnrollment(klassId);
		PaymentCallbackRequest request = new PaymentCallbackRequest(
			"pay-1",
			enrollmentId,
			BigDecimal.valueOf(10000)
		);

		mockMvc.perform(post("/api/payments/callback")
				.header(IDEMPOTENCY_KEY_HEADER, "callback-key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());
	}

	@Test
	void callbackRejectsSameKeyAndDifferentPayload() throws Exception {
		Long klassId = createOpenKlass("Payment callback conflict API");
		Long enrollmentId = createEnrollment(klassId);
		PaymentCallbackRequest request = new PaymentCallbackRequest(
			"pay-1",
			enrollmentId,
			BigDecimal.valueOf(10000)
		);

		mockMvc.perform(post("/api/payments/callback")
				.header(IDEMPOTENCY_KEY_HEADER, "callback-key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());

		PaymentCallbackRequest conflictRequest = new PaymentCallbackRequest(
			"pay-2",
			enrollmentId,
			BigDecimal.valueOf(10000)
		);

		mockMvc.perform(post("/api/payments/callback")
				.header(IDEMPOTENCY_KEY_HEADER, "callback-key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(conflictRequest)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
	}

	private Long createOpenKlass(String title) throws Exception {
		Long klassId = createKlass(title);

		mockMvc.perform(patch("/api/klasses/{klassId}/open", klassId)
				.header(USER_ID_HEADER, CREATOR_ID))
			.andExpect(status().isNoContent());

		return klassId;
	}

	private Long createKlass(String title) throws Exception {
		LocalDateTime startDate = LocalDateTime.of(2026, 8, 1, 0, 0);
		CreateKlassRequest request = new CreateKlassRequest(
			title,
			title + " description",
			BigDecimal.valueOf(10000),
			10,
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

	private Long createEnrollment(Long klassId) throws Exception {
		String response = mockMvc.perform(post("/api/klasses/{klassId}/enrollments", klassId)
				.header(USER_ID_HEADER, USER_ID))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return objectMapper.readTree(response).get("id").asLong();
	}
}
