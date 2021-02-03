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

    @Id @GeneratedValue
    private Long unmannedPaymentKioskId;

    private Long gateId;

    private String kioskName;                    // 키오스크 이

    private Integer paymentKioskType;       // 정산기기 유형  출구 1, 사전 2

    private String couponType;              // 할인권 취급유형

    private String installDevice;           // 설치 장비

    private Long accountId;                 // 계정 아이디
    private String discountTypes;              // 할인권 취급유형
    private String setupDevices;               // 설치 장비

    @Column(nullable = false)
    private String kioskIp;                    //키오스크 IP

    @Column(nullable = false)
    private String kioskPort;                   // 키오스크 port

    private String cardPaymentMachinePort;           // 카드 결제기 Port
    private String discountVoucherRecognizerPort;    // 할인권 인식기
    private String qrCodeRecognizerPort;               // QR코드 인식기
    private String tMoneyPaymentMachinePort;         // t머니 결제기
    private String receiptPrinterPort;               // 영수증 프린터

    @Override
    public Long getId() {
        return unmannedPaymentKioskId;
    }
}
