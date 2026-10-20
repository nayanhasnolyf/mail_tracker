package com.applicationtracker.repository;

import com.applicationtracker.domain.SyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncStateRepository extends JpaRepository<SyncState, UUID> {

    Optional<SyncState> findByUserId(UUID userId);
}
