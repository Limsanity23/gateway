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

import com.bnids.core.base.AuditModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author chang
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Reservation extends AuditModel<Long> {
    @Id
    @Column
    @GeneratedValue
    private Long reservationId;

//    @Column(nullable = false, length = 20)
//    private String siteCode;

    @Column(nullable = false)
    private Integer appUserId;

    @Column(nullable = true)
    private Long visitCarId;

    @Column(nullable = false, length = 10)
    private String visitCarNo;

    @Column(nullable = false, length = 10,name = "digit_car_no")
    private String digitCarNo;

    @Column(nullable = false, length = 200)
    private String note;

    @Column(nullable = false)
    private LocalDateTime accessPeriodBeginDt;

    @Column(nullable = false)
    private LocalDateTime accessPeriodEndDt;

    @JsonIgnore
    @Override
    public Long getId() {
        return reservationId;
    }

    @PrePersist
    public void PrePersist() {
        digitCarNo = visitCarNo.replaceAll("[^0-9]","");
    }
}
