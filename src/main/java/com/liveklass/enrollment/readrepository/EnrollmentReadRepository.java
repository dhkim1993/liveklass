package com.liveklass.enrollment.readrepository;

import com.liveklass.enrollment.readservice.EnrollmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentReadRepository {

	Page<EnrollmentDto> findByUserId(Long userId, Pageable pageable);

	Page<EnrollmentDto> findByKlassId(Long klassId, Pageable pageable);
}
