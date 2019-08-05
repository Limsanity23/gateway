package com.bnids.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
@Builder
public class InterlockResponseDto {
    private String code;
    private String message;
}
