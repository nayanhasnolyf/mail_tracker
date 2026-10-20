package com.applicationtracker.repository;

import com.applicationtracker.domain.ProcessedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, UUID> {

    boolean existsByUserIdAndMessageId(UUID userId, String messageId);

    Optional<ProcessedEmail> findByUserIdAndMessageId(UUID userId, String messageId);
}
