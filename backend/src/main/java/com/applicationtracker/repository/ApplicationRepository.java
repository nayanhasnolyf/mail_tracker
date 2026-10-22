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

    List<Application> findByUserIdAndCompanyIgnoreCase(UUID userId, String company);

    Optional<Application> findFirstByUserIdAndCompanyIgnoreCaseOrderByFirstSeenAtDesc(UUID userId, String company);

    Optional<Application> findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(UUID userId, String company, String role);

    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT a FROM Application a
        WHERE a.user.id = :userId
          AND (:status IS NULL OR a.currentStatus = :status)
          AND (:companySearch IS NULL OR LOWER(a.company) LIKE LOWER(CONCAT('%', :companySearch, '%')))
    """)
    org.springframework.data.domain.Page<Application> findByFilters(
        @org.springframework.data.repository.query.Param("userId") UUID userId,
        @org.springframework.data.repository.query.Param("status") ApplicationStatus status,
        @org.springframework.data.repository.query.Param("companySearch") String companySearch,
        org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT a FROM Application a
        WHERE a.user.id = :userId
          AND a.currentStatus = com.applicationtracker.domain.ApplicationStatus.APPLIED
          AND a.lastUpdatedAt <= :threshold
        ORDER BY a.lastUpdatedAt ASC
    """)
    List<Application> findPendingFollowups(
        @org.springframework.data.repository.query.Param("userId") UUID userId,
        @org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold
    );
}
