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

import com.bnids.gateway.entity.AppVisitCar;
import com.bnids.gateway.entity.RegistCar;
import com.bnids.gateway.entity.Reservation;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author yannishin
 */
@Setter @Getter
@Builder
public class InterlockRequestDto {
    private String carNo;
    private String lprCarNo;
    private Long registCarId;
    private Long carSection;
    private Long gateId;
    private String gateName;
    private Integer gateType;
    private Integer gateStatus;
    private String carImage;
    private Integer plateType;
    private String telNo;
    private String visitName;
    private String addressDong;
    private String addressHo;
    private String noticeSetup;
    private String installOption;
    private String installDevice;
    private String siteCode;
    private String ownerName;
    private String leaveCarRestrictionUseYn;
    private Date visitAllowableTime;
    private boolean paymentSuccess;
    private LocalDateTime warningCarDeleteDt;
    private Integer gatePaymentType;
    private Integer transitMode;
    private Long unmannedPaymentKioskId;
    private String note;
    private String carImageColor;


    public void setBy(RegistCar registCar) {
        this.setRegistCarId(registCar.getRegistCarId());
        this.setCarNo(registCar.getCarNo());
        this.setCarSection(registCar.getRegistItem());
        this.setTelNo(registCar.getTelNo());
        this.setVisitName(registCar.getOwnerName());
        this.setAddressDong(registCar.getAddressDong());
        this.setAddressHo(registCar.getAddressHo());
        this.setNoticeSetup(registCar.getNoticeSetup());
        this.setOwnerName(registCar.getOwnerName());
    }

    public void setBy(AppVisitCar appVisitCar) {
        this.setCarSection(3L);
        this.setTelNo(appVisitCar.getVisitTelNo());
        this.setVisitName(appVisitCar.getVisitorName());
        this.setAddressDong(appVisitCar.getAddressDong());
        this.setAddressHo(appVisitCar.getAddressHo());
    }

    public void setByReservation(Reservation reservation) {
        this.setCarSection(3L);
    }

    @Override
    public String toString() {
        return "Request{" +
                "carNo='" + carNo + '\'' +
                ", lprCarNo='" + lprCarNo + '\'' +
                ", registCarId=" + registCarId +
                ", carSection='" + carSection + '\'' +
                ", gateId=" + gateId +
                ", gateName=" + gateName +
                ", gateType=" + gateType +
                ", gateStatus='" + gateStatus + '\'' +
                ", plateType='" + plateType + '\'' +
                ", telNo='" + telNo + '\'' +
                ", visitName='" + visitName + '\'' +
                ", addressDong='" + addressDong + '\'' +
                ", addressHo='" + addressHo + '\'' +
                ", noticeSetup='" + noticeSetup + '\'' +
                ", installOption=" + installOption +
                ", installDevice=" + installDevice +
                ", siteCode=" + siteCode +
                ", ownerName=" + ownerName +
                ", leaveCarRestrictionUseYn=" + leaveCarRestrictionUseYn +
                ", visitAllowableTime=" + visitAllowableTime +
                ", paymentSuccess=" + paymentSuccess +
                ", warningCarDeleteDt=" + warningCarDeleteDt +
                ", gatePaymentType=" + gatePaymentType +
                ", transitMode=" + transitMode +
                ", unmannedPaymentKioskId=" + unmannedPaymentKioskId +
                ", note=" + note +
                '}';
    }
}

