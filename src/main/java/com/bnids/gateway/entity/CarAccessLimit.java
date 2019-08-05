/*
 * BNIndustry Inc., Software License, Version 1.0
 *
 * Copyright (c) 2018 BNIndustry Inc.,
 * All rights reserved.
 *
 *  DON'T COPY OR REDISTRIBUTE THIS SOURCE CODE WITHOUT PERMISSION.
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL <<BNIndustry Inc.>> OR ITS
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *
 *  For more information on this product, please see www.bnids.com
 */

package com.bnids.gateway.entity;

import com.bnids.core.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @author yannishin
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table
public class CarAccessLimit extends BaseEntity<Long> {
    @Id
    @GeneratedValue
    @Column
    private Long carAccessLimitId;

    @Column(length = 1000)
    private String dayLimit;

    @Column(length = 1000)
    private String dateLimit;

    @Column
    private LocalDate limitBeginDate;

    @Column
    private LocalDate limitEndDate;

    @Column
    private LocalTime limitBeginTime;

    @Column
    private LocalTime limitEndTime;

    @Column(length = 1, nullable = false)
    private String operationLimitExceptYn;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regist_car_id", nullable = false)
    //@MapsId
    private RegistCar registCar;

    public void setRegistCar(final RegistCar registCar) {
        //if (this.registCar != null) {
        //    this.registCar.getCarAccessLimit()
        //}

        this.registCar = registCar;
    }

    @JsonIgnore
    @Override
    public Long getId() {
        return carAccessLimitId;
    }
}
