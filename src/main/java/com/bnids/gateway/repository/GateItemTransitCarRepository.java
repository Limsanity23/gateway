package com.bnids.gateway.repository;

import com.bnids.core.base.BaseJPARepository;
import com.bnids.gateway.entity.GateItemTransitCar;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

public interface GateItemTransitCarRepository extends BaseJPARepository<GateItemTransitCar, Long> {
    //@Query("select u from GateItemTransitCar u where u.gateId=:gateId and u.registItemId=:registItemId order by u.gateId asc")
    Optional<GateItemTransitCar> findByGateIdAndRegistItemId(@Param("gateId") Long gateId, @Param("registItemId") Long registItemId);
}