package com.liveklass.klass.commandservice;

import com.liveklass.klass.commandrepository.KlassRepository;
import com.liveklass.klass.domain.Klass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class KlassCommandService {

	private final KlassRepository klassRepository;

	public Long create(CreateKlassDto dto) {
		Klass klass = Klass.create(
			dto.creatorId(),
			dto.title(),
			dto.description(),
			dto.price(),
			dto.capacity(),
			dto.startDate(),
			dto.endDate()
		);

		return klassRepository.save(klass).getId();
	}

	public void open(Long klassId, Long requesterId) {
		Klass klass = klassRepository.getByIdOrThrow(klassId);

		klass.open(requesterId);
	}

	public void close(Long klassId, Long requesterId) {
		Klass klass = klassRepository.getByIdOrThrow(klassId);

		klass.close(requesterId);
	}
}
