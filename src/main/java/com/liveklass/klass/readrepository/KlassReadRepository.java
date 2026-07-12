package com.liveklass.klass.readrepository;

import com.liveklass.klass.domain.enums.KlassStatus;
import com.liveklass.klass.readservice.KlassDto;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KlassReadRepository {

	Page<KlassDto> findPage(KlassStatus status, Pageable pageable);

	Optional<KlassDto> findById(Long klassId);
}
