package com.liveklass.enrollment.read.service;

import com.liveklass.enrollment.read.repository.EnrollmentReadRepository;
import com.liveklass.klass.command.repository.KlassRepository;
import com.liveklass.klass.domain.Klass;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentReadService {

	private final KlassRepository klassRepository;
	private final EnrollmentReadRepository enrollmentReadRepository;

	public Page<EnrollmentDto> getMyList(Long userId, Pageable pageable) {
		return enrollmentReadRepository.findByUserId(userId, pageable);
	}

	public Page<EnrollmentDto> getListByKlassId(Long klassId, Long requesterId, Pageable pageable) {
		Klass klass = klassRepository.getByIdOrThrow(klassId);
		klass.validateCreator(requesterId);

		return enrollmentReadRepository.findByKlassId(klassId, pageable);
	}
}
