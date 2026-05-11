package com.example.demo.user.domain;

import com.example.demo.common.entity.SoftDeleteBaseEntity;
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
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", nullable = false, length = 20)
    private MaritalStatus maritalStatus;

    @Column(name = "children_count", nullable = false)
    private Integer childrenCount;

    @Column(name = "is_homeless", nullable = false)
    private boolean homeless;

    @Column(name = "is_low_income", nullable = false)
    private boolean lowIncome;

    @Column(name = "is_elderly", nullable = false)
    private boolean elderly;

    @Column(name = "preferred_region_level1", length = 50)
    private String preferredRegionLevel1;

    @Column(name = "preferred_region_level2", length = 50)
    private String preferredRegionLevel2;

    @Column(name = "preferred_house_type", length = 50)
    private String preferredHouseType;

    @Column(name = "preferred_supply_type", length = 50)
    private String preferredSupplyType;

    @Column(name = "max_deposit")
    private Long maxDeposit;

    @Column(name = "max_monthly_rent")
    private Long maxMonthlyRent;

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
