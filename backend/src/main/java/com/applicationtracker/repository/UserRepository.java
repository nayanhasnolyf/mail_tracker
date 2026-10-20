package com.applicationtracker.repository;

import com.applicationtracker.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSubjectId(String googleSubjectId);

    boolean existsByEmail(String email);
}
