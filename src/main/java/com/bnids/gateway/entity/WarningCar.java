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
 * @author yannishin
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class WarningCar extends AuditModel<Long> {
    @Id
    @GeneratedValue
    @Column
    private Long warningCarId;

    @Column(length = 20, nullable = false)
    private String carNo;

    @Column(length = 10)
    private String digitCarNo;

    @Column(length = 100)
    private String carKind;

    @Column(length = 100)
    private String registReason;

    @Column(nullable = false)
    private int registStatus;

    @Column(length = 100)
    private String warningCarRegistRequestName;

    @Column
    private LocalDateTime warningCarRegistRequestDt;

    @Column
    private LocalDateTime DeletedDt;

    @JsonIgnore
    @Override
    public Long getId() {
        return warningCarId;
    }

    @Column
    //20211001 cks 현재 해당 컬럼은 row 저장시 별도 값 지정되지 않고 null로만 저장되고 있음.
    //사용되고 있지 않은 컬럼. 조회시 에러발생하여 enum에서 string 으로 변경함.
//    @Column(precision = 9, nullable = true)
//    @Enumerated(value = EnumType.STRING)
//    private WarningCarRegistEnum registKind;
    private String registKind;

    @Column
    private String registMethodKind;

    @Column(length = 500)
    private String warningCarRegistContent;

    @Column(precision = 9)
    private Long carSection;

    @Column(precision = 9)
    private Integer lastVisitCarId;

    @Column
    private String register;
}
