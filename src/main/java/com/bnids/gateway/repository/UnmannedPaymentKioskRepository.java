package com.bnids.gateway.repository;

import com.bnids.core.base.BaseJPARepository;
import com.bnids.gateway.entity.UnmannedPaymentKiosk;

import java.util.Optional;

public interface UnmannedPaymentKioskRepository extends BaseJPARepository<UnmannedPaymentKiosk, Long> {
    Optional<UnmannedPaymentKiosk> findByGateId(Long gateId);
}
