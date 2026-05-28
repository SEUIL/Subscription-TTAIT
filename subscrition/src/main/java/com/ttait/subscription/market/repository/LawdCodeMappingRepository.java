package com.ttait.subscription.market.repository;

import com.ttait.subscription.market.domain.LawdCodeMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LawdCodeMappingRepository extends JpaRepository<LawdCodeMapping, Long> {

    Optional<LawdCodeMapping> findFirstByRegionLevel2AndLegalDongNameAndActiveTrue(
            String regionLevel2,
            String legalDongName);

    Optional<LawdCodeMapping> findFirstByRegionLevel2AndLegalDongNameAndLegalDongCode(
            String regionLevel2,
            String legalDongName,
            String legalDongCode);
}
