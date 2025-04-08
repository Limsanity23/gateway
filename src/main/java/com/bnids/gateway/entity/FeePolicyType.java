package com.bnids.gateway.entity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum FeePolicyType {
    NORMAL,
    WEEKEND,
    // 방문확인 사용
    VISIT_CONFIRM,
    // 입주자 방문확인 사용
    RESIDENT_CONFIRM,
    ;
}