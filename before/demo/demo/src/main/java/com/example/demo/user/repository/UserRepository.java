package com.example.demo.user.repository;

import com.example.demo.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginIdAndDeletedFalse(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
}
