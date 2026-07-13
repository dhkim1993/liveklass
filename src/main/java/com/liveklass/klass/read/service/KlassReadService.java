package com.liveklass.klass.read.service;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.klass.domain.enums.KlassStatus;
import com.liveklass.klass.read.repository.KlassReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KlassReadService {

	private final KlassReadRepository klassReadRepository;

	public Page<KlassDto> getList(KlassStatus status, Pageable pageable) {
		return klassReadRepository.findPage(status, pageable);
	}

	public KlassDto getOne(Long klassId) {
		return klassReadRepository.findById(klassId)
			.orElseThrow(() -> new LiveKlassException(ErrorCode.KLASS_NOT_FOUND));
	}
}
