package com.bnids.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@Builder
public class SignageServerRequestDto {
    private String carNo;
    private Long gateId;
    private Integer registItemId;
}
