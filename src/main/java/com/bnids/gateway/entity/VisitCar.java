package com.bnids.gateway.entity;

import com.bnids.core.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@Builder
//@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class VisitCar extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long visitCarId;

    @Column(length = 20, nullable = false)
    private String carNo;

    @Column(length = 20, nullable = false)
    private String lprCarNo;

    @Column(precision = 9, nullable = false)
    private Integer carSection;

    @Column(precision = 9, nullable = false)
    private Integer entranceGateId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime entvhclDt;

    @Column(precision = 9, nullable = false)
    private Integer entranceGateStatus;

    @Column(length = 100)
    private String entranceWorker;

    @Column(precision = 9)
    private Integer exitGateId;

    @Column
    private LocalDateTime lvvhclDt;

    @Column(precision = 9)
    private Integer exitGateStatus;

    @Column(length = 100)
    private String exitWorker;

    @Column(length = 20)
    private String telNo;

    @Column(length = 10)
    private String visitDong;

    @Column(length = 10)
    private String visitHo;

    @Column(length = 100)
    private String visitPurpose;

    @Column(length = 100)
    private String visitPlaceName;

    @Column
    private Date visitAllowableTime;

    @JsonIgnore
    @Override
    public Long getId() {
        return visitCarId;
    }

    @Column(precision = 9, nullable = false)
    private Integer restrictLeaveCar;

    @Column
    private Integer entranceGateTransitMode;

    @Column
    private Integer exitGateTransitMode;

}
