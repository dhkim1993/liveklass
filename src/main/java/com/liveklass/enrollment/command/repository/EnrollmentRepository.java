package com.liveklass.enrollment.command.repository;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.domain.Enrollment;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByKlassIdAndUserIdAndStatusIn(
            Long klassId,
            Long userId,
            Collection<EnrollmentStatus> statuses
    );

    long countByKlassIdAndStatusIn(Long klassId, Collection<EnrollmentStatus> statuses);

    default Enrollment getByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new LiveKlassException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}
