package com.bnids.gateway.repository;

import com.bnids.core.base.BaseJPARepository;
import com.bnids.gateway.entity.VisitCar;

import java.util.Optional;

public interface VisitCarRepository extends BaseJPARepository<VisitCar, Long> {
    Optional<VisitCar> findById(Long id);
    Optional<VisitCar> findTopByCarNoAndLvvhclDtIsNullOrderByEntvhclDtDesc(String carNo);
}
