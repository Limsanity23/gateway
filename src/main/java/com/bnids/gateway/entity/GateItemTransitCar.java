package com.bnids.gateway.entity;

import com.bnids.core.base.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;

/**
 * 통로-아이템별 통과차량 설정
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "gate_item_transit_car")
public class GateItemTransitCar extends AuditModel<Long> {
  private static final long serialVersionUID = 1L;

  /**
   * 통로별 통과차량 설정 고유번호
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "gate_item_transit_car_id", insertable = false, nullable = false)
  private Long gateItemTransitCarId;

  /**
   * 통로고유번호
   */
  @Column(name = "gate_id", nullable = false)
  private Long gateId;

  /**
   * 등록항목 고유번호
   */
  @Column(name = "regist_item_id", nullable = false)
  private Long registItemId;

  /**
   * 등록항목 통과 가능여부. Y/N
   */
  @Builder.Default
  @Column(name = "item_transit_yn", nullable = false)
  private String itemTransitYn = "Y";


  @JsonIgnore
  @Override
  public Long getId() {
    return gateItemTransitCarId;
  }
}