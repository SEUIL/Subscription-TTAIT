package com.example.demo.market.repository;

import com.example.demo.market.domain.LawdCodeMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LawdCodeMappingRepository extends JpaRepository<LawdCodeMapping, Long> {
    Optional<LawdCodeMapping> findBySidoNameAndSigunguNameAndActiveTrue(String sidoName, String sigunguName);
}
