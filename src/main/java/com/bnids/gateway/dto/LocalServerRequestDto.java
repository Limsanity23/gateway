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
package com.bnids.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yannishin
 */
@Setter @Getter
@Builder
public class LocalServerRequestDto {
    private String carNo;
    private String lprCarNo;
    private Long gateId;
    private String gateName;
    private Integer gateType;
    private Long carSection;
    private Integer gateStatus;
    private String carImage;
    private String telNo;
    private String visitPlaceName;
    private String addressDong;
    private String addressHo;
    private String siteCode;
    private String installOption;
    private Integer restrictLeaveCar;
    private Integer transitMode;
    private String note;
    private String carImageColor;
}
