package com.bnids.gateway.entity;

import com.bnids.core.base.AuditModel;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class UnmannedPaymentKiosk extends AuditModel<Long> {

    @Id
    @GeneratedValue
    private Long unmannedPaymentKioskId;

    private Long gateId;

    private String kioskName;                    // 키오스크 이

    private String code;                    // 유료정산기기 코드

    private Integer paymentKioskType;       // 정산기기 유형  출구 1, 사전 2

    private Integer used;                   // 사용 여부    사용: 1, 사용안함 2

    private String couponType;              // 할인권 취급유형

    private String installDevice;           // 설치 장비

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private String port;

    private Long accountId;                 // 계정 아이디

    @Override
    public Long getId() {
        return unmannedPaymentKioskId;
    }
}
