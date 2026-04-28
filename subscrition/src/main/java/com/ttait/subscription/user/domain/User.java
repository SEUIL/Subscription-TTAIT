package com.ttait.subscription.user.domain;

import com.ttait.subscription.common.entity.SoftDeleteBaseEntity;
import com.ttait.subscription.user.domain.enums.Role;
import com.ttait.subscription.user.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeleteBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (자동 증가)

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId; // 로그인 ID (중복 불가)

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // 비밀번호 해시 (bcrypt)

    @Column(nullable = false, length = 30)
    private String phone; // 휴대폰 번호

    @Column(nullable = false, unique = true, length = 100)
    private String email; // 이메일 (중복 불가)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status; // 계정 상태 (활성/비활성)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10) NOT NULL DEFAULT 'USER'")
    private Role role; // 권한 (USER/ADMIN)

    @Builder
    public User(String loginId, String passwordHash, String phone, String email, UserStatus status, Role role) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.role = role != null ? role : Role.USER;
    }
}
