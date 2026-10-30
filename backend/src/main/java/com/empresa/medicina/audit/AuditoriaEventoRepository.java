package com.empresa.medicina.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditoriaEventoRepository
		extends JpaRepository<AuditoriaEvento, Long>, JpaSpecificationExecutor<AuditoriaEvento> {

}
