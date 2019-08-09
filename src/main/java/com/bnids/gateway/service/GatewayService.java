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

package com.bnids.gateway.service;

import com.bnids.exception.NotFoundException;
import com.bnids.gateway.dto.LprRequestDto;
import com.bnids.gateway.entity.*;
import com.bnids.gateway.repository.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;

import java.nio.file.LinkOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author yannishin
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayService {
    @NonNull
    private final SystemSetupRepository systemSetupRepository;

    @NonNull
    private final RegistCarRepository registCarRepository;

    @NonNull
    private final CarAccessLimitRepository carAccessLimitRepository;

    @NonNull
    private final AppVisitCarRepository appVisitCarRepository;

    @NonNull
    private final WarningCarRepository warningCarRepository;

    //@NonNull
    //private final RegistItemRepository registItemRepository;

    @NonNull
    private final GateItemTransitCarRepository gateItemTransitCarRepository;

    @NonNull
    private final GateRepository gateRepository;

    @NonNull
    private final InterlockService interlockService;

    public void interlock(LprRequestDto requestDto) {

        Long gateId = requestDto.getGateId();
        Long registCarId = null;
        Integer accuracy = requestDto.getAccuracy();
        String carNo = requestDto.getCarNo();
        String lprCarNo = requestDto.getCarNo();

        log.info("CarNo = {}, gateId = {} 출입 시작",carNo,gateId);

        SystemSetup systemSetup = findSystemSetup(1L);
        Integer logicType = systemSetup.getLogicType();
        Integer operationLimitSetup = systemSetup.getOperationLimitSetup();

        Gate gate = findGate(gateId);

        Integer gateType = gate.getGateType();
        Integer transitMode = gate.getTransitMode();

        Long carSection;

        if (accuracy == 0) { // 미인식
            carSection = 1L;
            if (transitMode == 3) {
                // 무조건 통과인 경우만 출입 허용
                accessAllowed(requestDto, lprCarNo, gateType, carSection);
            } else {
                // 이외 출입 차단
                accessBlocked(requestDto, lprCarNo, gateType, carSection);
            }
        } else { // 인식, 오인식
            String telNo = null;
            String visitName = null;

            if (transitMode == 1) { // 획인후 통과

                boolean isAllowPass = false;

                if (isTexi(requestDto.getCarNo())) {
                    carSection = 7L;
                    isAllowPass = isAllowPass(carNo, registCarId, gateId, carSection, logicType, operationLimitSetup);
                } else {
                    RegistCar registCar = findRegistItem(carNo);

                    if (registCar == null) {
                        // 에약 방문 차량 조회
                        LocalDateTime currentTime = LocalDateTime.now();
                        AppVisitCar appVisitCar = appVisitCarRepository.findByVisitCarNoAndAccessPeriodBeginDtAfterAndAccessPeriodEndDtBefore(carNo, currentTime, currentTime);

                        if (appVisitCar == null) {
                            carSection = 2L;
                        } else {
                            carSection = 3L;
                            telNo = appVisitCar.getVisitTelNo();
                            visitName = appVisitCar.getVisitorName();
                            isAllowPass = isAllowPass(carNo, registCarId, gateId, carSection, logicType, operationLimitSetup);
                        }
                    } else {
                        registCarId = registCar.getRegistCarId();
                        carSection = registCar.getRegistItem();

                        telNo = registCar.getTelNo();
                        visitName = registCar.getOwnerName();
                        isAllowPass = isAllowPass(carNo, registCarId, gateId, carSection, logicType, operationLimitSetup);
                    }

                    if (isAllowPass) {
                        boolean isWarningCar = isWarningCar(carNo);

                        if (isWarningCar) {
                            // 출입 차단
                            carSection = 6L;
                            accessBlocked(requestDto, lprCarNo, gateType, carSection, telNo, visitName);
                        } else {
                            // 출입허용
                            accessAllowed(requestDto, lprCarNo, gateType, carSection, telNo, visitName);
                        }
                    } else {
                        // 출입 차단
                        accessBlocked(requestDto, lprCarNo, gateType, carSection, telNo, visitName);
                    }
                }
            } else {
                RegistCar registCar = findRegistItem(carNo);

                if (registCar == null) {
                    carSection = 2L;
                } else {
                    carSection = registCar.getRegistItem();

                    telNo = registCar.getTelNo();
                    visitName = registCar.getOwnerName();
                }

                if (transitMode == 2) { // 인식후 통과
                    boolean isOperationLimit = isCarAccessLimit(carNo, registCarId, logicType, operationLimitSetup);
                    boolean isWarningCar = isWarningCar(carNo);

                    if (isWarningCar) {
                        carSection = 6L;
                        accessBlocked(requestDto, lprCarNo, gateType, carSection, telNo, visitName);
                    } else {
                        accessAllowed(requestDto, lprCarNo, gateType, carSection, telNo, visitName);
                    }
                } else { // 무조건 통과
                    accessAllowed(requestDto, lprCarNo, gateType, carSection, telNo, visitName);
                }
            }
        }
    }

    /**
     *
     * @param systemSetupId
     * @return
     */
    private SystemSetup findSystemSetup(Long systemSetupId) {
        return systemSetupRepository.findById(systemSetupId)
                .orElseThrow(() -> new NotFoundException(String.format("시스템 설정정보가 존재하지 않습니다.[systemSetUpId:%d]",systemSetupId)));
    }

    /**
     *
     * @param gateId
     * @return
     */
    private Gate findGate(Long gateId) {
        return gateRepository.findById(gateId)
                .orElseThrow(() -> new NotFoundException(String.format("통로 존재하지 않습니다.[gateId:%d]",1L)));
    }

    /**
     * 등록 차량 여부
     *
     * @param carNo
     * @return
     */
    private RegistCar findRegistItem(String carNo) {
        // @todo 로직 적용
        return registCarRepository.findByCarNo(carNo);

    }

    /**
     * 앱 방문 차량
     *
     * @param carNo
     * @return
     */
    private AppVisitCar findAppVisitCar(String carNo) {
        LocalDateTime today = LocalDateTime.now();
        return appVisitCarRepository.findByVisitCarNoAndAccessPeriodBeginDtAfterAndAccessPeriodEndDtBefore(carNo, today, today);
    }

    /**
     * 차량통과 여부(통로별 통과 + 차량툴입제한)
     * @param gateId
     * @param registItemId
     * @return
     */
    private boolean isAllowPass(String carNo, Long registCarId, Long gateId, Long registItemId, Integer logicType, Integer operationLimitSetup) {
        return isGateItemTransitCar(gateId, registItemId) && !isCarAccessLimit(carNo, registCarId, logicType, operationLimitSetup);
    }


    /**
     * 통로별 통과 야부
     *
     * @param gateId
     * @param registItemId
     * @return
     */
    private boolean isGateItemTransitCar(final Long gateId, final Long registItemId) {
        return  gateItemTransitCarRepository.findByGateIdAndRegistItemId(gateId, registItemId)
                .map(gateItemTransitCar -> {
                    if ("Y".equalsIgnoreCase(gateItemTransitCar.getItemTransitYn())) {
                        return true;
                    }
                    return false;
                }).orElse(false);
    }

    /**
     * 차량 출입 제한
     *
     * @param carNo
     * @param registCarId
     * @param logicType
     * @param operationLimitSetup
     * @return
     */
    private boolean isCarAccessLimit(String carNo, final Long registCarId, Integer logicType, Integer operationLimitSetup) {
        return carAccessLimitRepository.findByRegistCarId(registCarId)
                .map(carAccessLimit -> {
                    if (logicType == 1) {
                        LocalDateTime currentTime = LocalDateTime.now();

                        // 요일제한
                        if (carAccessLimit.getDayLimit() != null) {
                            String[] dayLimits = StringUtils.split(carAccessLimit.getDayLimit(), ",");

                            for (String dayLimit : dayLimits) {
                                if (currentTime.getDayOfWeek().getValue() == NumberUtils.toInt(dayLimit)) {
                                    return true;
                                }
                            }
                        }

                        // 일자제한
                        if (carAccessLimit.getDateLimit() != null) {
                            String[] dateLimits = StringUtils.split(carAccessLimit.getDateLimit(), ",");

                            for (String dateLimit : dateLimits) {
                                if (currentTime.getDayOfMonth() == NumberUtils.toInt(dateLimit)) {
                                    return true;
                                }
                            }
                        }

                        //제한 일자
                        if (currentTime.isAfter(carAccessLimit.getLimitBeginDate().atTime(0, 1))
                                && currentTime.isBefore(carAccessLimit.getLimitEndDate().atTime(23, 59))) {
                            return true;
                        }

                        // 제한시간
                        LocalTime limitBeginTime = carAccessLimit.getLimitBeginTime();
                        LocalTime limitEndTime = carAccessLimit.getLimitEndTime();

                        LocalDateTime beginDateTime = limitBeginTime.atDate(LocalDate.now());
                        LocalDateTime endDateTime = limitEndTime.atDate(LocalDate.now());
                        if (limitBeginTime.isAfter(limitEndTime)) {
                            endDateTime = endDateTime.plusDays(1);
                        }
                        if ((currentTime.isAfter(beginDateTime) || currentTime.isEqual(beginDateTime))
                                && (currentTime.isBefore(endDateTime) || currentTime.isEqual(endDateTime))) {
                            return true;
                        }
                    }

                    // 부제 운행
                    if (operationLimitSetup > 0) {
                        if ("N".equals(carAccessLimit.getOperationLimitExceptYn())) {
                            String digit = carNo.replaceAll("[^0-9]", "");
                            int postfix = NumberUtils.toInt(StringUtils.right(digit, 1));
                            int prefix = NumberUtils.toInt(StringUtils.left(digit, 2));

                            // 승합, 외교, 군용 제외
                            if (prefix < 70 &&
                                    !(carNo.contains("외") || carNo.contains("영") || carNo.contains("-")) &&
                                    !(carNo.contains("합") || carNo.contains("육") || carNo.contains("해") || carNo.contains("공"))) {

                                if (operationLimitSetup == 5) { // 5부제
                                    int dayOfWeek = LocalDateTime.now().getDayOfWeek().getValue();

                                    if ((dayOfWeek == postfix) || ((dayOfWeek + 5) == postfix)) {
                                        return true;
                                    }

                                    /*
                                    switch (LocalDateTime.now().getDayOfWeek().getValue()) {
                                        case 1: //월요일
                                            if (postfix == 1 || postfix == 6) {
                                                return true;
                                            }
                                            break;

                                        case 2:
                                            if (postfix == 2 || postfix == 7) {
                                                return true;
                                            }
                                            break;

                                        case 3:

                                            if (postfix == 3 || postfix == 8) {
                                                return true;
                                            }
                                            break;

                                        case 4:

                                            if (postfix == 4 || postfix == 9) {
                                                return true;
                                            }
                                            break;

                                        case 5: // 금요일
                                            if (postfix == 0 || postfix == 5) {
                                                return true;
                                            }
                                            break;
                                    }
                                    */

                                } else {
                                    int day = LocalDateTime.now().getDayOfMonth();
                                    if (day < 30) { // 31일은 부제 적용 예외
                                        if (operationLimitSetup == 2) { // 2부제
                                            if ((day % 2) != (postfix % 2)) { // positive 채택
                                                return true;
                                            }

                                        } else if (operationLimitSetup == 10) {
                                            if ((day % 10) == postfix) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return false;
                }).orElse(false);
    }

    private boolean isWarningCar(String carNo) {
        return warningCarRepository.existsByCarNo(carNo);
    }

    /**
     * 출입 허용
     *
     * @param dto
     * @param lprCarNo
     * @param gateType
     * @param carSection
     */
    private void accessAllowed(LprRequestDto dto, String lprCarNo, Integer gateType, Long carSection) {
        accessAllowed(dto, lprCarNo, gateType, carSection, "", "");
    }


    /**
     * 출입 허용
     *
     * @param dto
     * @param lprCarNo
     * @param gateType
     * @param carSection
     * @param telNo
     * @param visitPlaceName
     */
    private void accessAllowed(LprRequestDto dto, String lprCarNo, Integer gateType, Long carSection, String telNo, String visitPlaceName) {
        interlockService.sendGateServer(dto.getGateId());
        interlockService.sendSignageServer(dto, carSection);
        interlockService.sendLocalServer(dto, lprCarNo, gateType, carSection, 1, telNo, visitPlaceName);
    }

    /**
     * 충입 차단
     *
     * @param dto
     * @param lprCarNo
     * @param gateType
     * @param carSection
     */
    private void accessBlocked(LprRequestDto dto, String lprCarNo, Integer gateType, Long carSection) {
        accessBlocked(dto, lprCarNo, gateType, carSection, "", "");
    }

    /**
     * 출입 차단
     *
     * @param dto
     * @param lprCarNo
     * @param gateType
     * @param carSection
     * @param telNo
     * @param visitPlaceName
     */
    private void accessBlocked(LprRequestDto dto, String lprCarNo, Integer gateType, Long carSection, String telNo, String visitPlaceName) {
        interlockService.sendSignageServer(dto, carSection);
        interlockService.sendLocalServer(dto, lprCarNo, gateType, carSection, 2, telNo, visitPlaceName);
    }

    /**
     * 택시 여부
     * @param carNo
     * @return
     */
    private boolean isTexi(String carNo) {
        String[] symbols = {"바", "사", "아", "자"};
        boolean findSymbol = false;

        for (String symbol : symbols) {
            if (carNo.contains(symbol)) {
                findSymbol = true;
                break;
            }
        }

        boolean isTaxi = false;

        if (findSymbol) {
            //log.info("regionStr="+ StringUtils.left(StringUtils.right(carNo,7),2));
            int region = NumberUtils.toInt( StringUtils.left(StringUtils.right(carNo,7),2), 0);
            //log.info("region="+region);

            if (region > 0 && region < 80) {
                isTaxi = true;
            }
        }

        return isTaxi;
    }

}
