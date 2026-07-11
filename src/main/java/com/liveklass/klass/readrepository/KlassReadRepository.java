package com.liveklass.klass.readrepository;

import com.liveklass.klass.domain.Klass;
import com.liveklass.klass.domain.enums.KlassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KlassReadRepository {

	Page<Klass> findPage(KlassStatus status, Pageable pageable);
}
