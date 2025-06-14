package com.bnids.gateway.repository;

import com.bnids.gateway.entity.WarningCarAutoRegistRules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarningCarAutoRegistRulesRepository extends JpaRepository<WarningCarAutoRegistRules, Long> {
    @Query("select w from WarningCarAutoRegistRules w where w.useYn = 'Y' and ( w.carSection = :carSection or w.carSection = 0 ) and (w.applyDt is null or w.applyDt < current_timestamp)")
    List<WarningCarAutoRegistRules> findByCarSection(@Param("carSection") Long carSection);

    @Query("select w from WarningCarAutoRegistRules w where w.useYn = 'Y' and (w.applyDt is null or w.applyDt < current_timestamp)")
    List<WarningCarAutoRegistRules> findRules();
}
