package com.example.demo.announcement.repository;

import com.example.demo.announcement.domain.RawPayload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawPayloadRepository extends JpaRepository<RawPayload, Long> {
}
