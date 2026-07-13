package com.liveklass.enrollment.read.repository;

import com.liveklass.enrollment.read.service.EnrollmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentReadRepository {

	Page<EnrollmentDto> findByUserId(Long userId, Pageable pageable);

	Page<EnrollmentDto> findByKlassId(Long klassId, Pageable pageable);
}
