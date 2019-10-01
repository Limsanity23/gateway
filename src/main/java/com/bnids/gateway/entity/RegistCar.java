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
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author yannishin
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
//@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class RegistCar extends AuditModel<Long> {
    @Id
    @GeneratedValue
    @Column
    private Long registCarId;

    @Column(precision = 3, nullable = false)
    private Integer registSection;

    @Column(precision = 3, nullable = false)
    private Long registItem;

    @Column(length = 20, nullable = false, unique = true)
    private String carNo;

    @Column(length = 10)
    private String digitCarNo;

    @Column
    private Integer appUserId;

    //@Convert(converter = ApvlStatusConverter.class)
    @Column(nullable = false)
    private Integer aprvlStatus;

    @Column(length = 1, nullable = false)
    private String carOwnYn;

    @Column(length = 1, nullable = false)
    private String accessPeriodUnlimitYn;

    @Column
    private LocalDateTime accessPeriodBeginDt;

    @Column
    private LocalDateTime accessPeriodEndDt;

    @Column(length = 20, nullable = false)
    private String ownerName;

    @Column(length = 20, nullable = false)
    private String telNo;

    @Column(length = 10, nullable = false)
    private String addressDong;

    @Column(length = 10, nullable = false)
    private String addressHo;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String carKind;

    @Column(length = 100)
    private String carColor;

    @Column(length = 1000)
    private String etc;

    @Column(length = 100)
    private String ownerCompany;

    @Column(length = 100)
    private String ownerPosition;

    @Column(length = 1000)
    private String noticeSetup;

    @Column(length = 100)
    private String visitKioskPassword;

    @Column
    private LocalDateTime deletedDt;

    @Override
    public Long getId() {
        return registCarId;
    }
}
