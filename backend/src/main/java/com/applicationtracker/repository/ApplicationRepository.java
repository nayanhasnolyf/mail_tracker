package com.applicationtracker.repository;

import com.applicationtracker.domain.Application;
import com.applicationtracker.domain.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByUserId(UUID userId);

    List<Application> findByUserIdAndCurrentStatus(UUID userId, ApplicationStatus status);

    Optional<Application> findByUserIdAndSourceEmailId(UUID userId, String sourceEmailId);

    boolean existsByUserIdAndSourceEmailId(UUID userId, String sourceEmailId);
}
