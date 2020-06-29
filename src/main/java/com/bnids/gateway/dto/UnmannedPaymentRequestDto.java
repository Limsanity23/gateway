package com.bnids.gateway.dto;

import lombok.Builder;

@Builder
public class UnmannedPaymentRequestDto {
    private Long visitCarId;
    private String carNo;
    private String gateName;
    private Integer gateType;
    private Long gateId;
}
