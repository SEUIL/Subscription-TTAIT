package com.ttait.subscription.external.lh;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LhImportCandidateRepository extends JpaRepository<LhImportCandidate, Long> {

    Optional<LhImportCandidate> findByPanId(String panId);

    Page<LhImportCandidate> findByStatus(LhImportCandidateStatus status, Pageable pageable);

    List<LhImportCandidate> findByIdIn(Collection<Long> ids);
}
