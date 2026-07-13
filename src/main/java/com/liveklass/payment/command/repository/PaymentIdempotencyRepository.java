package com.liveklass.payment.command.repository;

import com.liveklass.payment.domain.PaymentIdempotency;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentIdempotencyRepository extends JpaRepository<PaymentIdempotency, Long> {

	Optional<PaymentIdempotency> findByIdempotencyKey(String idempotencyKey);
}
