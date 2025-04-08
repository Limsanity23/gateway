package com.bnids.gateway.entity;

import com.bnids.core.base.AuditModel;
import com.bnids.gateway.enums.CouponType;
import lombok.*;

import javax.persistence.*;

@Entity
@Builder
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Coupon extends AuditModel<Long> {

    @Id
    @GeneratedValue
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "discount")
    private long discount;

    private Integer price;

    private String name;

    private String couponType;                  // 쿠폰 타입, HOUR 냐 PERCENT

    private String useYn;

    @Override
    public Long getId() {
        return couponId;
    }

    @Builder.Default
    private String delYn = "N";

    private Integer couponSection;                  // 쿠폰유형 1: 상가 방문 할인, 2: 입주자 방문 할인

}
