package com.bnids.gateway.repository;

import com.bnids.gateway.entity.FeePolicy;
import com.bnids.gateway.entity.FeePolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeePolicyRepository  extends JpaRepository<FeePolicy, Long> {

    Optional<FeePolicy> findByFeePolicyTypeAndUseYn(FeePolicyType feePolicyType, String useYn);
}
