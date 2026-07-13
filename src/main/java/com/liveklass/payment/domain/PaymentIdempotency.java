package com.liveklass.payment.domain;

import com.liveklass.common.BaseTimeEntity;
import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.payment.domain.enums.PaymentIdempotencyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "payment_idempotencies",
	indexes = {
		@Index(name = "idx_payment_idempotencies_key", columnList = "idempotency_key", unique = true)
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentIdempotency extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "idempotency_key", nullable = false, length = 200, unique = true)
	private String idempotencyKey;

	@Column(name = "request_hash", nullable = false, length = 64)
	private String requestHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentIdempotencyStatus status;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	private PaymentIdempotency(String idempotencyKey, String requestHash) {
		this.idempotencyKey = idempotencyKey;
		this.requestHash = requestHash;
		this.status = PaymentIdempotencyStatus.PROCESSING;
	}

	public static PaymentIdempotency processing(String idempotencyKey, String requestHash) {
		return new PaymentIdempotency(idempotencyKey, requestHash);
	}

	public boolean isCompleted() {
		return status == PaymentIdempotencyStatus.COMPLETED;
	}

	public void validateSameRequest(String requestHash) {
		if (!this.requestHash.equals(requestHash)) {
			throw new LiveKlassException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
		}
	}

	public void validateNotProcessing() {
		if (status == PaymentIdempotencyStatus.PROCESSING) {
			throw new LiveKlassException(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);
		}
	}

	public void complete(LocalDateTime completedAt) {
		this.status = PaymentIdempotencyStatus.COMPLETED;
		this.completedAt = completedAt;
	}
}
