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
import com.bnids.gateway.dto.InterlockRequestDto;
import com.bnids.gateway.dto.LprRequestDto;
import com.bnids.gateway.entity.*;
import com.bnids.gateway.repository.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

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

    @NonNull
    private final GateItemTransitCarRepository gateItemTransitCarRepository;

    @NonNull
    private final GateRepository gateRepository;

    @NonNull
    private final InterlockService interlockService;

    public void interlock(LprRequestDto lprRequestDto) {
        Integer accuracy = lprRequestDto.getAccuracy();
        String lprCarNo = accuracy == 0 ? "미인식" : lprRequestDto.getLprCarNo();
        String carNo = lprCarNo;
        Long gateId = lprRequestDto.getGateId();


        SystemSetup systemSetup = findSystemSetup();
        Integer logicType = systemSetup.getLogicType();
        Integer operationLimitSetup = systemSetup.getOperationLimitSetup();

        Gate gate = findGate(gateId);

        String gateName = gate.getGateName();

        Integer transitMode = gate.getTransitMode();

        log.info("차량번호 = {}, 통로 = {}({}) 출입 시작",carNo,gateName, gateId);
        long beforeTime = System.currentTimeMillis();

        InterlockRequestDto requestDto = InterlockRequestDto.builder()
                .lprCarNo(lprCarNo)
                .carNo(lprCarNo)
                .gateId(gateId)
                .gateName(gate.getGateName())
                .gateType(gate.getGateType())
                .installOption(gate.getInstallDevice())
                .carImage(lprRequestDto.getCarImage())
                .plateType(lprRequestDto.getPlateType())
                .siteCode(systemSetup.getSiteCode()).build();

        if (StringUtils.contains(lprCarNo, "미인식")) {
            requestDto.setCarNo("미인식"+System.currentTimeMillis());
            requestDto.setCarSection(1L);
            if (transitMode == 3) {
                // 무조건 통과인 경우만 출입 허용
                accessAllowed(requestDto);
            } else {
                // 이외 출입 차단
                accessBlocked(requestDto);
            }
        } else { // 인식, 오인식
            if (transitMode == 1) { // 획인후 통과
                boolean isAllowPass = false;
                RegistCar registCar = findRegistCar(carNo, logicType);

                if (registCar == null) {
                    long taxiType = getTaxiType(carNo);

                    if (taxiType > 0) {
                        requestDto.setCarSection(taxiType);
                        isAllowPass = isAllowPass(requestDto, logicType, operationLimitSetup);
                    } else {
                        // 에약 방문 차량 조회
                        LocalDateTime currentTime = LocalDateTime.now();
                        AppVisitCar appVisitCar = appVisitCarRepository.findByVisitCarNoAndAccessPeriodBeginDtBeforeAndAccessPeriodEndDtAfter(carNo, currentTime.minusHours(1), currentTime.plusHours(1));

                        if (appVisitCar == null) {
                            requestDto.setCarSection(2L);
                        } else {

                            requestDto.setCarSection(3L);
                            requestDto.setTelNo(appVisitCar.getVisitTelNo());
                            requestDto.setVisitName(appVisitCar.getVisitorName());
                            requestDto.setAddressDong(appVisitCar.getAddressDong());
                            requestDto.setAddressHo(appVisitCar.getAddressHo());
                            isAllowPass = isAllowPass(requestDto, transitMode, operationLimitSetup);
                        }
                    }
                } else {
                    requestDto.setRegistCarId(registCar.getRegistCarId());
                    requestDto.setCarSection(registCar.getRegistItem());
                    requestDto.setTelNo(registCar.getTelNo());
                    requestDto.setVisitName(registCar.getOwnerName());
                    requestDto.setAddressDong(registCar.getAddressDong());
                    requestDto.setAddressHo(registCar.getAddressHo());
                    requestDto.setNoticeSetup(registCar.getNoticeSetup());

                    isAllowPass = isAllowPass(requestDto, transitMode, operationLimitSetup);
                }


                if (isAllowPass) {
                    boolean isWarningCar = isWarningCar(carNo);

                    if (isWarningCar) {
                        // 출입 차단
                        requestDto.setCarSection(6L);
                        accessBlocked(requestDto);
                    } else {
                        // 출입허용
                        accessAllowed(requestDto);
                    }
                } else {
                    // 출입 차단
                    accessBlocked(requestDto);
                }

            } else {
                RegistCar registCar = findRegistCar(carNo, logicType);

                if (registCar == null) {
                    requestDto.setCarSection(2L);
                } else {
                    requestDto.setRegistCarId(registCar.getRegistCarId());
                    requestDto.setCarSection(registCar.getRegistItem());
                    requestDto.setTelNo(registCar.getTelNo());
                    requestDto.setVisitName(registCar.getOwnerName());
                    requestDto.setAddressDong(registCar.getAddressDong());
                    requestDto.setAddressHo(registCar.getAddressHo());
                    requestDto.setNoticeSetup(registCar.getNoticeSetup());
                }

                if (transitMode == 2) { // 인식후 통과
                    boolean isOperationLimit = isCarAccessLimit(requestDto, transitMode, operationLimitSetup);
                    boolean isWarningCar = isWarningCar(carNo);

                    if (!isOperationLimit && isWarningCar) {
                        requestDto.setCarSection(6L);
                        accessBlocked(requestDto);
                    } else {
                        accessAllowed(requestDto);
                    }
                } else { // 무조건 통과
                    accessAllowed(requestDto);
                }
            }
        }

        long afterTime = System.currentTimeMillis();
        long elapseTime  = afterTime - beforeTime;

        if (elapseTime > 1000) {
            log.info("Lazy Log : 차량번호 = {} {} ms", requestDto.getCarNo(), elapseTime);
        }
    }

    /**
     * 시스템 설정정보 조회
     * @return 시스템 설정
     */
    private SystemSetup findSystemSetup() {
        log.info("시스템 설정 정보 조회");
        return systemSetupRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new NotFoundException("시스템 설정정보가 존재하지 않습니다."));
    }

    /**
     * 통로가 조회
     * @param gateId 통로고유번호
     * @return 통로
     */
    private Gate findGate(Long gateId) {
        log.info("통로 정보 조회 gateId = {}",gateId);
        return gateRepository.findById(gateId)
                .orElseThrow(() -> new NotFoundException(String.format("통로가  존재하지 않습니다.[gateId:%d]",1L)));
    }

    /**
     * 등록 차량 정보
     *
     * @param carNo 차량번호
     * @return 등록차량
     */
    private RegistCar findRegistCar(String carNo, Integer logicType) {
        LocalDateTime now = LocalDateTime.now();
        if (logicType == 99) {
            log.info("등록 차량 조회(완전일치로직) 차량번호 = {}",carNo);
            return registCarRepository.findByCarNoAndAprvlStatusAndAccessPeriodBeginDtBeforeAndAccessPeriodEndDtAfter(carNo, 1, now, now);
        } else {
            return findRegistCarByDigitCarNo(carNo, now);
        }
    }

    /**
     * 등록 차량 정보
     *
     * @param carNo 차량번호
     * @return 등록차량
     */
    private RegistCar findRegistCarByDigitCarNo(String carNo, LocalDateTime now) {
        String digitCarNo = digitCarNo(carNo);
        log.info("등록 차량 조회(숫자일치로직) 차량번호 = {}({})", carNo, digitCarNo);
        List<RegistCar> registCarList = registCarRepository.findByDigitCarNoEndsWithAndAprvlStatusAndAccessPeriodBeginDtBeforeAndAccessPeriodEndDtAfter(digitCarNo,1, now, now);
        //Stream<RegistCar> registCarStream = registCarList.stream();

        if (registCarList.isEmpty()) {
            return null;
        }

        if(registCarList.size() == 1) { // 결과가 1개이면
            log.info("차량번호 = {}({}) : 1건 조회됨", carNo, digitCarNo);
            return registCarList.get(0);
        } else { // 결과가 2개 이상
            Stream<RegistCar> stream = registCarList.stream().filter(registCar -> carNo.equals(registCar.getCarNo()));

            if (stream.count() == 0) { // 완전히 일차하는 차량이 없을 경우
                log.info("차량번호 = {}({}) : 다건 조회후 완전일치 차량 없음 ", carNo, digitCarNo);
                return registCarList.get(0);
            } else { // 완전히 일차하는 차량이 존재
                log.info("차량번호 = {}({}) : 다건 조회후 완전일치 차량 있음 ", carNo, digitCarNo);
                return stream.findFirst().get();
            }
        }
    }

    /**
     * 앱 방문 차량
     *
     * @param carNo 차량번호
     * @return 앱 방문 차량
     */
    private AppVisitCar findAppVisitCar(String carNo) {
        LocalDateTime today = LocalDateTime.now();
        return appVisitCarRepository.findByVisitCarNoAndAccessPeriodBeginDtAfterAndAccessPeriodEndDtBefore(carNo, today, today);
    }

    /**
     *
     * 차량통과 여부(통로별 통과 + 차량출입제한)
     *
     * @param requestDto 연동요청 Dto
     * @param transitMode 통과모드
     * @param operationLimitSetup 운핼제한 설정
     * @return 차량통과 여부
     */
    private boolean isAllowPass(InterlockRequestDto requestDto, Integer transitMode, Integer operationLimitSetup) {
        return isGateItemTransitCar(requestDto) && !isCarAccessLimit(requestDto, transitMode, operationLimitSetup);
    }


    /**
     * 통로별 통과 야부
     *
     * @param requestDto 연동요청 Dto
     * @return 통로별 통과 야부
     */
    private boolean isGateItemTransitCar(InterlockRequestDto requestDto) {
        return  gateItemTransitCarRepository.findByGateIdAndRegistItemId(requestDto.getGateId(), requestDto.getCarSection())
                .map(gateItemTransitCar -> {
                    if ("Y".equalsIgnoreCase(gateItemTransitCar.getItemTransitYn())) {
                        log.info("차량번호 = {}, 통로 = {}({}) 통로별통과 차량, 통과됨",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                        return true;
                    } else {
                        log.info("차량번호 = {}, 통로 = {}({}) 통로별통과 차량, 차단됨",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                        return false;
                    }
                }).orElseGet(() ->{
                    log.info("차량번호 = {}, 통로 = {}({}) 통로별통과 차량 설정이 안되어 있어 차단됨",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                    return false;
                });
    }

    /**
     * 차량 출입 제한
     *
     * @param requestDto 연동요청 Dto
     * @param transitMode 통과모드
     * @param operationLimitSetup 운핼제한 설정
     * @return 차량 출입 제한 여부
     */
    private boolean isCarAccessLimit(InterlockRequestDto requestDto, Integer transitMode, Integer operationLimitSetup) {
        log.info("차량번호 = {}, 통로 = {}({}) 출입제한 시작",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
        return carAccessLimitRepository.findByRegistCarId(requestDto.getRegistCarId())
                .map(carAccessLimit -> {
                    if (transitMode == 1) {
                        LocalDateTime currentTime = LocalDateTime.now();

                        // 요일제한
                        if (carAccessLimit.getDayLimit() != null) {
                            String[] dayLimits = StringUtils.split(carAccessLimit.getDayLimit(), ",");

                            for (String dayLimit : dayLimits) {
                                if (currentTime.getDayOfWeek().getValue() == NumberUtils.toInt(dayLimit)) {
                                    log.info("차량번호 = {}, 통로 = {}({}) 출입제한됨, 요일 적용 ",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                                    return true;
                                }
                            }
                        }

                        // 일자제한
                        if (carAccessLimit.getDateLimit() != null) {
                            String[] dateLimits = StringUtils.split(carAccessLimit.getDateLimit(), ",");

                            for (String dateLimit : dateLimits) {
                                if (currentTime.getDayOfMonth() == NumberUtils.toInt(dateLimit)) {
                                    log.info("차량번호 = {}, 통로 = {}({}) 출입제한됨, 날짜 적용 ",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                                    return true;
                                }
                            }
                        }

                        //제한 일자
                        if (carAccessLimit.getLimitBeginDate() == null || carAccessLimit.getLimitEndDate() == null) {
                            log.info("차량번호 = {}, 통로 = {}({}) 출입제한, 미적용으로 통과됨",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                            return false;
                        }


                        if (currentTime.isAfter(carAccessLimit.getLimitBeginDate().atTime(0, 1))
                                && currentTime.isBefore(carAccessLimit.getLimitEndDate().atTime(23, 59))) {
                            log.info("차량번호 = {}, 통로 = {}({}) 출입제한됨, 일자 적용 ",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                            return true;
                        }

                        // 제한시간
                        if (carAccessLimit.getLimitBeginTime() == null || carAccessLimit.getLimitEndTime() == null) {
                            log.info("차량번호 = {}, 통로 = {}({}) 출입제한, 미적용으로 통과됨",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                            return false;
                        }

                        LocalTime limitBeginTime = carAccessLimit.getLimitBeginTime();
                        LocalTime limitEndTime = carAccessLimit.getLimitEndTime();

                        LocalDateTime beginDateTime = limitBeginTime.atDate(LocalDate.now());
                        LocalDateTime endDateTime = limitEndTime.atDate(LocalDate.now());
                        if (limitBeginTime.isAfter(limitEndTime)) {
                            endDateTime = endDateTime.plusDays(1);
                        }
                        if ((currentTime.isAfter(beginDateTime) || currentTime.isEqual(beginDateTime))
                                && (currentTime.isBefore(endDateTime) || currentTime.isEqual(endDateTime))) {
                            log.info("차량번호 = {}, 통로 = {}({}) 출입제한됨, 시간 적용",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                            return true;
                        }
                        log.info("차량번호 = {}, 통로 = {}({}) 출입제한, 미적용으로 통과됨",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId());
                        return false;
                    }

                    // 부제 운행
                    String carNo = requestDto.getCarNo();
                    if (operationLimitSetup > 0) {
                        if ("N".equals(carAccessLimit.getOperationLimitExceptYn())) {

                            String digit = digitCarNo(carNo);
                            int postfix = NumberUtils.toInt(StringUtils.right(digit, 1));
                            int prefix = NumberUtils.toInt(StringUtils.left(digit, 2));

                            // 승합, 외교, 군용 제외
                            if (prefix < 70 &&
                                    !(carNo.contains("외") || carNo.contains("영") || carNo.contains("-")) &&
                                    !(carNo.contains("합") || carNo.contains("육") || carNo.contains("해") || carNo.contains("공"))) {

                                if (operationLimitSetup == 5) { // 5부제
                                    int dayOfWeek = LocalDateTime.now().getDayOfWeek().getValue();

                                    if ((dayOfWeek == postfix) || ((dayOfWeek + 5) == postfix)) {
                                        log.info("차량번호 = {}, 통로 = {}({}) 부제 운행 제한, 5부제 적용됨", carNo, requestDto.getGateName(), requestDto.getGateId());
                                        return true;
                                    }
                                } else {
                                    int day = LocalDateTime.now().getDayOfMonth();
                                    if (day < 30) { // 31일은 부제 적용 예외
                                        if (operationLimitSetup == 2) { // 2부제
                                            if ((day % 2) != (postfix % 2)) { // positive 채택
                                                log.info("차량번호 = {}, 통로 = {}({}) 부제 운행 제한, 2부제 적용됨",carNo,requestDto.getGateName(), requestDto.getGateId());
                                                return true;
                                            }

                                        } else if (operationLimitSetup == 10) { // 10부제
                                            if ((day % 10) == postfix) {
                                                log.info("차량번호 = {}, 통로 = {}({}) 부제 운행 제한, 10부제 적용됨",carNo,requestDto.getGateName(), requestDto.getGateId());
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        log.info("차량번호 = {}, 통로 = {}({}) 부제 운행 제한, 미적용으로 통과됨",carNo,requestDto.getGateName(), requestDto.getGateId());
                        return false;
                    }

                    log.info("차량번호 = {}, 통로 = {}({}) 부제 운행 제한, 미적용으로 통과됨",carNo,requestDto.getGateName(), requestDto.getGateId());
                    return false;
                }).orElseGet(() -> {
                    log.info("차량번호 = {}, 통로 = {}({}) , 출입제한, 미적용으로 통과됨 ",requestDto.getCarNo(), requestDto.getGateName(), requestDto.getGateId());
                    return false;
                });
    }

    /**
     * 경고차량 여부
     *
     * @param carNo 차량번호
     * @return 경고차량 여부
     */
    private boolean isWarningCar(String carNo) {

        boolean warningCar = warningCarRepository.existsByCarNo(carNo);
        if (warningCar) {
            log.info("차량번호 = {} 경고 차량차량으로 출입 차단됨",carNo);
        }
        return warningCar;
    }

    /**
     * 출입 허용
     *
     * @param requestDto 연동요청 Dto
     */
    private void accessAllowed(InterlockRequestDto requestDto) {
        requestDto.setGateStatus(1);
        interlockService.sendGateServer(requestDto);
        interlockService.sendSignageServer(requestDto);
        interlockService.sendLocalServer(requestDto);
        interlockService.sendHomenetServer(requestDto);
    }

    /**
     * 출입 차단
     *
     * @param requestDto 연동요청 Dto
     */
    private void accessBlocked(InterlockRequestDto requestDto) {
        requestDto.setGateStatus(2);
        interlockService.sendSignageServer(requestDto);
        interlockService.sendLocalServer(requestDto);
    }

    /**
     * 택시 유형
     * @param carNo 차량번호
     * @return 택시 유형
     */
    private long getTaxiType(String carNo) {
        long taxiType = 0L;

        String[] symbols = {"바", "사", "아", "자", "배"};
        boolean findSymbol = false;

        for (String symbol : symbols) {
            if (carNo.contains(symbol)) {
                findSymbol = true;
                break;
            }
        }

        if (findSymbol) {
            int region = NumberUtils.toInt( StringUtils.left(StringUtils.right(carNo,7),2), 0);

            if (region > 0 && region < 70) {
                taxiType = 7L;
            } else if (region > 70 && region < 80) {
                taxiType = 8L;
            } else if (region >= 80 && region < 99) {
                taxiType = 9L;
            }
        }

        return taxiType;
    }

    private String digitCarNo(String carNo) {
        return carNo.replaceAll("[^0-9]", "");
    }
}
