package com.bnids.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@Builder
public class LprRequestDto {
    private String carNo;
    private Integer accuracy;
    private String carImage;
    private Long gateId;
    private Integer gateType;
}
