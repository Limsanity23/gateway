/*
 *  BNIndustry Inc., Software License, Version 1.0
 *
 *   Copyright (c) 2019. BNIndustry Inc.,
 *   All rights reserved.
 *
 *    DON'T COPY OR REDISTRIBUTE THIS SOURCE CODE WITHOUT PERMISSION.
 *    THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *    WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *    DISCLAIMED. IN NO EVENT SHALL <<BNIndustry Inc.>> OR ITS
 *    CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *    SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *    LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *    USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *    ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *    OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *    SUCH DAMAGE.
 *
 *    For more information on this product, please see www.bnids.com
 */
package com.bnids.gateway.entity;

import com.bnids.core.base.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;

/**
 * @author yannishin
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