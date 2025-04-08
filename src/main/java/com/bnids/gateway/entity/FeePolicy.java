package com.bnids.gateway.entity;


import com.bnids.core.base.AuditModel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.time.LocalTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Slf4j
@Table(name = "fee_policy")
public class FeePolicy extends AuditModel<Long> {

    @Id
    @GeneratedValue
    @Column(name = "fee_policy_id")
    private Long feePolicyId;

    @Column(name = "basic_fee")
    private int basicFee;

    @Column(name = "basic_fee_minutes")
    private int basicFeeMinutes;

    @Column(name = "additional_fee")
    private int additionalFee;

    @Column(name = "additional_fee_minutes")
    private int additionalFeeMinutes;

    @Column(name = "car_section")
    private int carSection;

    // 무료 주차 시간
    @Column(name = "free_parking_time")
    private long freeParkingTime;

    // 여유 시간
    @Builder.Default
    @Column(name = "free_time")
    private long freeTime = 0;

    // 주차비 예외시간
    @Column(name = "basic_fee_use_yn")
    private String basicFeeUseYn;                               // 기본요금 사용여부

    @Column(name = "additional_fee_use_yn")
    private String additionalFeeUseYn;                          // 추가요금 사용여부

    @Column(name = "free_parking_time_use_yn")
    private String freeParkingTimeUseYn;                        // 무료시간 사용여부

    @Column(name = "free_time_use_yn")
    private String freeTimeUseYn;                               // 여유시간 사용여부

//    @Enumerated(EnumType.STRING)
//    @Column(name = "parking_fee_method")
//    private ParkingFeeMethod parkingFeeMethod;                  // 주차비 정산방식

    @Column(name = "fee_policy_type")
    @Enumerated(EnumType.STRING)
    private FeePolicyType feePolicyType;

    // 할인 할증 설정
    @Column(name = "discount_extra_charge_use_yn")
    private String discountExtraChargeUseYn;                    // 주차 할인 할증 사용 여부

    @Column(name = "discount_extra_basic_fee_minutes")
    private int discountExtraBasicFeeMinutes;                   // 기준 주차 시간

    @Column(name = "discount_extra_additional_fee_minutes")
    private int discountExtraAdditionalFeeMinutes;              // 단위 시간

    @Column(name = "discount_extra_additional_fee")
    private int discountExtraAdditionalFee;                     // 단위당 주차비

    @Column(name = "except_time_use_yn")
    private String exceptTimeUseYn;

    @Column(name = "start_free_time")
    private LocalTime startFreeTime;

    @Column(name = "end_free_time")
    private LocalTime endFreeTime;

    @Column(name = "use_yn")
    private String useYn;

    @Column(name = "one_day_maximum_fee_price_use_yn")
    private String oneDayMaximumFeePriceUseYn;

    @Column(name = "one_day_maximum_fee_price")
    private long oneDayMaximumFeePrice;

    @Transient
    @Builder.Default
    private Long timeBeCalculated = 0L;

    @Transient
    @Builder.Default
    private Long remainingMinutes = 0L;

    @Transient
    @Builder.Default
    private Boolean isFirstPolicy = false;

    @Transient
    private final static int ONE_DAY_MINUTES = 1440;

    public void checkFirstPolicy() {
        this.isFirstPolicy = true;
    }

    @Override
    public Long getId() {
        return feePolicyId;
    }
}