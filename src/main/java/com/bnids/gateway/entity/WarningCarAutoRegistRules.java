package com.bnids.gateway.entity;

import com.bnids.core.base.AuditModel;
import lombok.*;

import javax.persistence.*;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class WarningCarAutoRegistRules extends AuditModel<Long> {

    @Id @GeneratedValue
    @Column(name = "warning_car_auto_regist_rules_id")
    private Long warningCarAutoRegistRulesId;

    @Column
    @Enumerated(EnumType.STRING)
    private WarningCarRegistEnum warinigCarRulesSection;

    @Column
    private Long carSection;

    @Column
    private Integer registDay;

    @Column
    private Integer parkingTime;

    @Column
    private Integer violationTime;

    @Column
    private String useYn;

    @Override
    public Long getId() {
        return getWarningCarAutoRegistRulesId();
    }
}
