package com.project.savingbee.domain.user.repository;

import com.project.savingbee.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Boolean existsByUsername(String username);

    Optional<User> findByUsernameAndIsLockAndIsSocial(String username, Boolean isLock, Boolean isSocial);
}
