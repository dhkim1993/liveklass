package com.liveklass.klass.commandrepository;

import com.liveklass.klass.domain.Klass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KlassRepository extends JpaRepository<Klass, Long> {
}
