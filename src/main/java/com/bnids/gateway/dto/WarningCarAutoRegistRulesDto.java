package com.bnids.gateway.dto;

import com.bnids.gateway.entity.WarningCarRegistEnum;
import com.bnids.gateway.entity.WarningCarAutoRegistRules;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Setter
@Getter
public class WarningCarAutoRegistRulesDto {
    private Long carSection;
    private Integer registDay;
    private Integer parkingTime;
    private Integer violationTime;
    private WarningCarRegistEnum warinigCarRulesSection;
    private String carNo;
    private LocalDateTime deletedDt;
    private LocalDateTime applyDt;
    private Integer parkingTimeMinutes;
    private Integer actualViolationCount;

    public WarningCarAutoRegistRulesDto of(WarningCarAutoRegistRules warningCarAutoRegistRules) {
        return WarningCarAutoRegistRulesDto.builder()
                    .carSection(warningCarAutoRegistRules.getCarSection())
                    .parkingTime(warningCarAutoRegistRules.getParkingTime())
                    .violationTime(warningCarAutoRegistRules.getViolationTime())
                    .warinigCarRulesSection(warningCarAutoRegistRules.getWarinigCarRulesSection())
                    .registDay(warningCarAutoRegistRules.getRegistDay())
                    .applyDt(warningCarAutoRegistRules.getApplyDt())
                .parkingTime(warningCarAutoRegistRules.getParkingTime())
                .parkingTimeMinutes(warningCarAutoRegistRules.getParkingTimeMinutes())
                .build();
    }

    @Override
    public String toString() {
        return "WarningCarAutoRegistRulesDto{" +
                "carSection=" + carSection +
                ", registDay=" + registDay +
                ", parkingTime=" + parkingTime +
                ", violationTime=" + violationTime +
                ", warinigCarRulesSection=" + warinigCarRulesSection +
                ", carNo='" + carNo + '\'' +
                ", deletedDt=" + deletedDt +
                ", applyDt=" + applyDt +
                ", parkingTimeMinutes=" + parkingTimeMinutes +
                ", actualViolationCount=" + actualViolationCount +
                '}';
    }
}
