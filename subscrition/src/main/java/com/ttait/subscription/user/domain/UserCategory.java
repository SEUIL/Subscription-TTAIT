package com.ttait.subscription.user.domain;

import com.ttait.subscription.common.entity.BaseTimeEntity;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCategory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (자동 증가)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 연관 사용자

    @Enumerated(EnumType.STRING)
    @Column(name = "category_code", nullable = false, length = 30)
    private CategoryCode categoryCode; // 해당하는 수혜 대상 카테고리

    @Builder
    public UserCategory(User user, CategoryCode categoryCode) {
        this.user = user;
        this.categoryCode = categoryCode;
    }
}
