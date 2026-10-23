package com.applicationtracker.repository;

import com.applicationtracker.domain.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {

    List<StatusHistory> findByApplicationIdOrderByChangedAtDesc(UUID applicationId);

    Optional<StatusHistory> findByApplicationIdAndSourceEmailId(UUID applicationId, String sourceEmailId);

    boolean existsByApplicationIdAndSourceEmailId(UUID applicationId, String sourceEmailId);

    @org.springframework.data.jpa.repository.Query("SELECT sh FROM StatusHistory sh WHERE sh.application.user.id = :userId ORDER BY sh.changedAt ASC")
    List<StatusHistory> findByUserIdOrderByChangedAtAsc(@org.springframework.data.repository.query.Param("userId") UUID userId);
}
