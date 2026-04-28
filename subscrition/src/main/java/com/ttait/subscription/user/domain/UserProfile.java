package com.ttait.subscription.user.domain;

import com.ttait.subscription.common.entity.SoftDeleteBaseEntity;
import com.ttait.subscription.user.domain.enums.MaritalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends SoftDeleteBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (자동 증가)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user; // 연관 사용자 (1:1)

    @Column(nullable = false)
    private Integer age; // 나이

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", nullable = false, length = 20)
    private MaritalStatus maritalStatus; // 혼인 상태

    @Column(name = "children_count", nullable = false)
    private Integer childrenCount; // 자녀 수

    @Column(name = "is_homeless", nullable = false)
    private boolean homeless; // 무주택 여부

    @Column(name = "is_low_income", nullable = false)
    private boolean lowIncome; // 저소득 여부

    @Column(name = "is_elderly", nullable = false)
    private boolean elderly; // 고령자 여부

    @Column(name = "preferred_region_level1", length = 50)
    private String preferredRegionLevel1; // 희망 거주 지역 (광역시/도)

    @Column(name = "preferred_region_level2", length = 50)
    private String preferredRegionLevel2; // 희망 거주 지역 (시/군/구)

    @Column(name = "preferred_house_type", length = 50)
    private String preferredHouseType; // 희망 주택 유형 (아파트/빌라 등)

    @Column(name = "preferred_supply_type", length = 50)
    private String preferredSupplyType; // 희망 공급 유형 (공공임대/분양 등)

    @Column(name = "max_deposit")
    private Long maxDeposit; // 최대 보증금 (단위: 만원)

    @Column(name = "max_monthly_rent")
    private Long maxMonthlyRent; // 최대 월세 (단위: 만원)

    @Builder
    public UserProfile(User user, Integer age, MaritalStatus maritalStatus, Integer childrenCount, boolean homeless,
                       boolean lowIncome, boolean elderly, String preferredRegionLevel1, String preferredRegionLevel2,
                       String preferredHouseType, String preferredSupplyType, Long maxDeposit, Long maxMonthlyRent) {
        this.user = user;
        this.age = age;
        this.maritalStatus = maritalStatus;
        this.childrenCount = childrenCount;
        this.homeless = homeless;
        this.lowIncome = lowIncome;
        this.elderly = elderly;
        this.preferredRegionLevel1 = preferredRegionLevel1;
        this.preferredRegionLevel2 = preferredRegionLevel2;
        this.preferredHouseType = preferredHouseType;
        this.preferredSupplyType = preferredSupplyType;
        this.maxDeposit = maxDeposit;
        this.maxMonthlyRent = maxMonthlyRent;
    }

    public void update(Integer age, MaritalStatus maritalStatus, Integer childrenCount, boolean homeless,
                       boolean lowIncome, boolean elderly, String preferredRegionLevel1, String preferredRegionLevel2,
                       String preferredHouseType, String preferredSupplyType, Long maxDeposit, Long maxMonthlyRent) {
        this.age = age;
        this.maritalStatus = maritalStatus;
        this.childrenCount = childrenCount;
        this.homeless = homeless;
        this.lowIncome = lowIncome;
        this.elderly = elderly;
        this.preferredRegionLevel1 = preferredRegionLevel1;
        this.preferredRegionLevel2 = preferredRegionLevel2;
        this.preferredHouseType = preferredHouseType;
        this.preferredSupplyType = preferredSupplyType;
        this.maxDeposit = maxDeposit;
        this.maxMonthlyRent = maxMonthlyRent;
    }
}
