package com.liveklass.enrollment.commandrepository;

import com.liveklass.enrollment.domain.Enrollment;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	boolean existsByKlassIdAndUserIdAndStatusIn(
		Long klassId,
		Long userId,
		Collection<EnrollmentStatus> statuses
	);

	Optional<Enrollment> findByIdAndUserId(Long id, Long userId);
}
