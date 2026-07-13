package com.liveklass.klass.read.repository;

import com.liveklass.klass.domain.enums.KlassStatus;
import com.liveklass.klass.read.service.KlassDto;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KlassReadRepository {

	Page<KlassDto> findPage(KlassStatus status, Pageable pageable);

	Optional<KlassDto> findById(Long klassId);
}
