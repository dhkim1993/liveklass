package com.liveklass.enrollment.readrepository;

import com.liveklass.enrollment.domain.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentReadRepository {

	Page<Enrollment> findByUserId(Long userId, Pageable pageable);

	Page<Enrollment> findByKlassId(Long klassId, Pageable pageable);
}
