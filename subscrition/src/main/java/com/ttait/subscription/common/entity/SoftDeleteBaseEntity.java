package com.ttait.subscription.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeleteBaseEntity extends BaseTimeEntity {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false; // 삭제 여부 (소프트 삭제)

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 삭제 처리 시각

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
