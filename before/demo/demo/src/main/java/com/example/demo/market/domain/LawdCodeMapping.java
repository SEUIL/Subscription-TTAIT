package com.example.demo.market.domain;

import com.example.demo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "lawd_code_mapping")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LawdCodeMapping extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sido_name", nullable = false, length = 50)
    private String sidoName;

    @Column(name = "sigungu_name", nullable = false, length = 50)
    private String sigunguName;

    @Column(name = "sigungu_code_5", nullable = false, length = 5)
    private String sigunguCode5;

    @Column(name = "full_code_10", length = 10)
    private String fullCode10;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder
    public LawdCodeMapping(String sidoName, String sigunguName, String sigunguCode5, String fullCode10,
                           boolean active) {
        this.sidoName = sidoName;
        this.sigunguName = sigunguName;
        this.sigunguCode5 = sigunguCode5;
        this.fullCode10 = fullCode10;
        this.active = active;
    }
}
