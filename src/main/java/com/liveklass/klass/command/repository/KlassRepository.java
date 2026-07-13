package com.liveklass.klass.command.repository;

import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.klass.domain.Klass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KlassRepository extends JpaRepository<Klass, Long> {

	default Klass getByIdOrThrow(Long id) {
		return findById(id)
			.orElseThrow(() -> new LiveKlassException(ErrorCode.KLASS_NOT_FOUND));
	}
}
