package com.empresa.medicina.tiers;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TierRepository extends JpaRepository<Tier, Long> {

	List<Tier> findByActivoTrueOrderByOrdenAccesoAsc();

	Optional<Tier> findByIdAndActivoTrue(Long id);

	Optional<Tier> findFirstByActivoTrueOrderByOrdenAccesoAsc();

	Optional<Tier> findFirstByActivoTrueOrderByOrdenAccesoDesc();

}
