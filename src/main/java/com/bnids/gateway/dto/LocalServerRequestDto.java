package com.bnids.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@Builder
public class LocalServerRequestDto {
    private String carNo;
    private String lprCarNo;
    private Long gateId;
    private Integer gateType;
    private Long carSection;
    private Integer gateStatus;
    private String carImage;
    private String telNo;
    private String visitPlaceName;
}
