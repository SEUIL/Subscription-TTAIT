package com.example.demo.user.repository;

import com.example.demo.user.domain.UserCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {
    List<UserCategory> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
