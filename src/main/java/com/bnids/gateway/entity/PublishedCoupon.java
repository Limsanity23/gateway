package com.bnids.gateway.entity;

import com.bnids.core.base.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PublishedCoupon extends AuditModel<Long> {
    @Id
    @GeneratedValue
    private Long publishedCouponId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_car_id")
    @JsonIgnore
    private VisitCar visitCar;                  // 방문차량

//    @Enumerated(EnumType.STRING)
//    private CouponStatus couponStatus;

    private String couponStatus;

    private LocalDateTime useDateTime;


    public Long getId() {
        return this.publishedCouponId;
    }
}
