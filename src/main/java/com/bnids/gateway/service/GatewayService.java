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
import com.bnids.gateway.dto.WarningCarAutoRegistRulesDto;
import com.bnids.gateway.dto.WarningCarDto;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
    private final LogicPatternRepository logicPatternRepository;

    @NonNull
    private final InterlockService interlockService;

    @NonNull
    private final VisitCarRepository visitCarRepository;

    @NonNull
    private final UnmannedPaymentKioskRepository unmannedPaymentKioskRepository;

    @NonNull
    private final WarningCarAutoRegistRulesRepository warningCarAutoRegistRulesRepository;

    @NonNull
    private final VisitCarRepositorySupport visitCarRepositorySupport;

    public void interlock(LprRequestDto lprRequestDto) {
        Integer accuracy = lprRequestDto.getAccuracy();
        if (accuracy == null) accuracy = 0;
        Integer accuracy2 = lprRequestDto.getAccuracy2();
        if (accuracy2 == null) accuracy2 = 0;
        Long gateId = lprRequestDto.getGateId();
        String carNo = lprRequestDto.getLprCarNo();
        String carNo2 = lprRequestDto.getLprCarNo2();
        boolean bothHaveNumber = false;

        log.info("@@ 인식엔진에서 넘어온 데이터 조회 {} ", lprRequestDto.toString());

        if (accuracy > 0 && accuracy2 > 0) { //둘다 인식
            bothHaveNumber = true;
        } else if (accuracy2 > 0) {
            carNo = lprRequestDto.getLprCarNo2();
        } else if (accuracy > 0) {
            carNo = lprRequestDto.getLprCarNo();
        } else { //둘다 미인식
            carNo = "미인식";
        }

        SystemSetup systemSetup = findSystemSetup();
        Integer logicType = systemSetup.getLogicType();
        Integer operationLimitSetup = systemSetup.getOperationLimitSetup();
        String leaveCarRestrictionUseYn = systemSetup.getLeaveCarRestrictionUseYn();
        Date visitAllowableTime = systemSetup.getVisitAllowableTime();
        Gate gate = findGate(gateId);

        String gateName = gate.getGateName();

        Integer transitMode = gate.getTransitMode();

        log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) 출입 시작",carNo,carNo2,gateName, gateId);
        long beforeTime = System.currentTimeMillis();

        InterlockRequestDto requestDto = InterlockRequestDto.builder()
                .lprCarNo(carNo)
                .carNo(carNo)
                .gateId(gateId)
                .gateName(gate.getGateName())
                .gateType(gate.getGateType())
                .installOption(systemSetup.getInstallOption())
                .installDevice(gate.getInstallDevice())
                .carImage(lprRequestDto.getCarImage())
                .plateType(lprRequestDto.getPlateType())
                .leaveCarRestrictionUseYn(leaveCarRestrictionUseYn)
                .visitAllowableTime(visitAllowableTime)
                .paymentSuccess(lprRequestDto.isPaymentSuccess())
                .gatePaymentType(gate.getGatePaymentType())
                .transitMode(transitMode)
                .siteCode(systemSetup.getSiteCode()).build();

        RegistCar registCar = findRegistCar(carNo, logicType);

        // 결제 미인식이어도 보내야 한다. 결제의 경우 미인식 ---- 미인식

        // 결제를 사용중이고 출구인 경우
        // 결제를 보낸다.
        if ("Y".equals(systemSetup.getPaymentEnabledYn()) && gate.getGateType() > 2) {
            if(registCar == null) {
                // 결제 후 통과 시작
                // 출입 차단

                if (StringUtils.contains(carNo, "미인식")) {
                    requestDto.setCarSection(1L);
                }

                Optional<UnmannedPaymentKiosk> unmannedPaymentKiosk = unmannedPaymentKioskRepository.findByGateId(requestDto.getGateId());
                unmannedPaymentKiosk.ifPresent(
                        paymentKiosk -> {
                            requestDto.setUnmannedPaymentKioskId(paymentKiosk.getId());
                        }
                );
                this.processAfterPayment(requestDto);
            } else {
                requestDto.setBy(registCar);
                accessAllowed(requestDto);
            }
        } else if (StringUtils.contains(carNo, "미인식")) {
            requestDto.setCarNo("미인식"+System.currentTimeMillis());
            requestDto.setCarSection(1L);
            if (transitMode == 3) {
                // 무조건 통과인 경우만 출입 허용
                accessAllowed(requestDto);
            }
        } else { // 인식, 오인식

            if (bothHaveNumber && !carNo.equals(carNo2)) { //두개 다 번호가 있지만, 그 두 번호가 같지 않은 경우에
                if (registCar == null) {
                    log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) {}가 검색되지 않음", carNo, carNo2, gateName, gateId, carNo);

                    RegistCar registCar2 = findRegistCar(carNo2, logicType);
                    // 두 번호 모두 등록되지 않은 경우 차량 번호의 길이가 지나치게 짧거나(4자리 이하) 숫자로 시작하지 않으면 무시
                    if (registCar2 == null && (carNo2.length() <= 4 || !Character.isDigit(carNo2.charAt(0)))) { //1110 or 울산53사1110
                        log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) {}도 검색되지 않음", carNo, carNo2, gateName, gateId, carNo2);
                        log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) {}은 패턴이 보편적이지 않아 선택하지 않음", carNo, carNo2, gateName, gateId, carNo2);
                    }else {
                        log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) {}를 선택함", carNo, carNo2, gateName, gateId, carNo2);
                        registCar = registCar2;
                        carNo = carNo2;
                        requestDto.setLprCarNo(carNo);
                        requestDto.setCarNo(carNo);
                        requestDto.setCarImage(lprRequestDto.getCarImage2());
                    }
                }
            }

            boolean isWarningCar = isWarningCar(carNo);
            if (transitMode == 1) { // 획인후 통과
                boolean isAllowPass = false;
                //
                if (isWarningCar) { // 경고 차량
                    requestDto.setCarSection(6L);
                } else if (registCar == null) {
                    long taxiType = getTaxiType(carNo);

                    if (taxiType > 0) {
                        requestDto.setCarSection(taxiType);
                        isAllowPass = isAllowPass(requestDto, logicType, operationLimitSetup);
                    } else {
                        // 에약 방문 차량 조회
                        AppVisitCar appVisitCar = this.findAppVisitCar(carNo);

                        if (appVisitCar == null) {
                            // 오인식 된 번호판 정보 => 부분일치, 임시로직 에 부합되는 등록 차량인지 판별, visit_car에도 기록
                            List<LogicPattern> logicPatterns = logicPatternRepository.findLogicPatternBycarNo(carNo);
                            if (logicPatterns.size() == 0) {
                                log.info("차량번호 = {}, 통로 = {}({}) 모든 개별로직에 부합하지 않음",carNo,gateName, gateId);
                                requestDto.setCarSection(2L);
                            } else {
                                final LogicPattern logicPattern = logicPatterns.get(0);
                                log.info("차량번호 = {}, 통로 = {}({}) 이 번호와 관련된 개별로직 갯수 {}",carNo,gateName, gateId, logicPatterns.size());
                                registCar = findRegistCar(logicPattern.getRegistCarId());
                                log.info("차량번호 = {}, 통로 = {}({}) LogicPattern: {}, 이 패턴으로 찾은 첫번째 차량번호: {}", carNo,gateName, gateId, logicPattern.getLogicPattern(), registCar.getCarNo());
                                requestDto.setBy(registCar);
                                isAllowPass = isAllowPass(requestDto, transitMode, operationLimitSetup);
                            }
                        } else {
                            requestDto.setBy(appVisitCar);
                            isAllowPass = isAllowPass(requestDto, transitMode, operationLimitSetup);
                        }
                    }
                } else { //registCar != null
                    requestDto.setBy(registCar);
                    if(isWarningCar || isRestrictedCar(requestDto)) {
                    } else {
                        isAllowPass = true;
                    }
                }

                if (isAllowPass) {
                    // 출입허용
                    accessAllowed(requestDto);
                } else {
                    // 출입 차단

                    // 출구인 경우 방문차량 주차시간 설정에 따른 예외 허용
                    if(gate.getGateType() == 3 && this.hasGlobalAllowableTime(requestDto)) { //글로벌 설정이 있는 상태에서
                        log.info("차량번호 = {}, 통로 = {}({}) 방문차량 주차시간 글로벌 설정 있음",carNo,gateName, gateId);
                        if (inAllowableTime(requestDto)) { // 제한시간 이내이면 허용
                            accessAllowed(requestDto);
                        } else { // 아니면 전광판에 표시
                            requestDto.setCarSection(100L); //주차시간초과 차량
                            accessBlocked(requestDto);
                        }
                    }else{
                        accessBlocked(requestDto);
                    }
                }

            } else {

                if (registCar == null) {
                    AppVisitCar appVisitCar = findAppVisitCar(carNo);

                    if (appVisitCar == null) {
                        requestDto.setCarSection(2L);
                    } else {
                        requestDto.setBy(appVisitCar);
                    }
                } else {
                    requestDto.setBy(registCar);
                }

                if (transitMode == 2) { // 인식후 통과
                    boolean isOperationLimit = isCarAccessLimit(requestDto, transitMode, operationLimitSetup);
                    if (!isOperationLimit && isWarningCar) {
                        requestDto.setCarSection(6L);
                        accessBlocked(requestDto);
                    } else {
                        accessAllowed(requestDto);
                    }
                } else {
                    if(isWarningCar) {
                        accessBlocked(requestDto);
                    } else {
                        accessAllowed(requestDto);
                    }
                }
            }
        }



        // 다른 모드인 경우를 구별해서 담아줄 필요가 있다.
        long afterTime = System.currentTimeMillis();
        long elapseTime  = afterTime - beforeTime;

        // 경고차량 삭제 차량인지 여부 확인
        // 결제 후 통과와 결제 후 통과가 아닌 경우를 생각해 보면
        // 단순하게 여기서 생각할건 넘겨야할

        // 삭제된 경고차량 여부
        List<WarningCar> warningCars = warningCarRepository.findWarningCarByCarNo(requestDto.getCarNo());
        WarningCar warningCar = warningCars.size() > 0 ? warningCars.get(0) : null;
        if(warningCar != null && warningCar.getRegistStatus() == 1) {
            requestDto.setWarningCarDeleteDt(warningCar.getDeletedDt());
        }

        WarningCarAutoRegistRulesDto autoRegistWarningCarRulesDto = this.autoRegistWarningCar(requestDto);
        if(autoRegistWarningCarRulesDto != null) {
            registAutoWarningCar(requestDto, autoRegistWarningCarRulesDto);
        }

        // 결제 후 통과가 아니여도 정산을 사용하면 요금계산은 되어야 한다.
        if (elapseTime > 1000) {
            log.info("Lazy Log : 차량번호 = {} {} ms", requestDto.getCarNo(), elapseTime);
        }
    }

    public void registAutoWarningCar(InterlockRequestDto requestDto, WarningCarAutoRegistRulesDto warningCarAutoRegistRulesDto) {
        WarningCarDto.Create create = WarningCarDto.Create.builder()
                .carNo(requestDto.getCarNo())
                .carSection(requestDto.getCarSection())
                .digitCarNo(digitCarNo(requestDto.getCarNo()))
                .registReason(warningCarAutoRegistRulesDto.getWarinigCarRulesSection().getValue())
                .registMethodKind("자동등록")
                .register("시스템")
                .build();
        warningCarRepository.save(create.toEntity());
    }

    public WarningCarAutoRegistRulesDto autoRegistWarningCar(InterlockRequestDto requestDto) {
        List<WarningCarAutoRegistRules> rules = warningCarAutoRegistRulesRepository.findByCarSection(requestDto.getCarSection());
        for(WarningCarAutoRegistRules rule : rules) {
            WarningCarAutoRegistRulesDto warningCarAutoRegistRules = WarningCarAutoRegistRulesDto
                    .builder()
                    .build()
                    .of(rule);
            warningCarAutoRegistRules.setCarNo(requestDto.getCarNo());
            if(requestDto.getWarningCarDeleteDt() != null) {
                warningCarAutoRegistRules.setDeletedDt(requestDto.getWarningCarDeleteDt());
            }
            if(visitCarRepositorySupport.findVisitCarListForRegistWarningCar(warningCarAutoRegistRules).size() >= rule.getViolationTime()) {
                return warningCarAutoRegistRules;
            }
        }
        return null;
    }

    public void processAfterPayment(InterlockRequestDto requestDto) {
        if(requestDto.isPaymentSuccess()) {
            accessAllowed(requestDto);
        } else {
            if(requestDto.getGatePaymentType() == 1) {
                interlockService.sendUnmannedPaymentServer(requestDto);
            } else {
                interlockService.sendMannedPaymentServer(requestDto);
            }
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
                .orElseThrow(() -> new NotFoundException(String.format("통로가 존재하지 않습니다.[gateId:%d]",1L)));
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
     * @param registCarId id값
     * @return 등록차량
     */
    private RegistCar findRegistCar(Long registCarId) {
        return registCarRepository.getOne(registCarId);
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
        if ("".equals(digitCarNo)) {
           return null;
        }

        List<RegistCar> registCarList = registCarRepository.findByDigitCarNoEndsWithAndAprvlStatusAndAccessPeriodBeginDtBeforeAndAccessPeriodEndDtAfter(digitCarNo,1, now, now);
        //Stream<RegistCar> registCarStream = registCarList.stream();

        if (registCarList.isEmpty()) {
            return null;
        }

        if(registCarList.size() == 1) { // 숫자일치 결과가 1개이면
            log.info("차량번호 = {}({}) : 숫자일치 1건 조회됨", carNo, digitCarNo);
            RegistCar registCar = registCarList.get(0);

            if (registCar.getCarNo().equals(carNo)) { //완전 일치
                log.info("차량번호 = {}({}) : 완전일치", carNo, digitCarNo);
                return registCar;
            }else{ //숫자만 일치
                // 조회된 차량이 완전 일치 강제 패턴에 등록되어 있나?
                if (this.hasExactCarNoPattern(registCar.getCarNo())) {
                    log.info("차량번호 = {}({}) : 인식된 이 번호는 완전일치 강제로 패턴 등록 되었으나 숫자만 일치됨 ", carNo, digitCarNo);
                    return null;
                }
            }
            log.info("차량번호 = {}({}) : 숫자 일치. 완전일치 강제 패턴 없음. ", carNo, digitCarNo);
            return registCar;

        } else { // 숫자일치 결과가 2개 이상

            Optional<RegistCar> registCarOptional = registCarList.stream().filter(registCar -> carNo.equals(registCar.getCarNo())).findFirst();

            if (registCarOptional.isPresent()) { // 완전히 일차하는 차량이  존재
                log.info("차량번호 = {}({}) : 숫자 일치 목록중 완전일치 차량 있음 ", carNo, digitCarNo);
                return registCarOptional.get();
            } else { // 완전히 일차하는 차량이 없음

                // 숫자일치 목록중 완전 일치 강제 패턴에 등록되지 않은 차량이 있나?
                Optional<RegistCar> registCarAvailable = registCarList.stream().filter(registCar -> !this.hasExactCarNoPattern(registCar.getCarNo())).findFirst();

                if (registCarAvailable.isPresent()) {
                    log.info("차량번호 = {}({}) : 숫자일치 목록중 완전 일치 강제 패턴에 등록되지 않은 차량 있음. 첫번째 선택.", registCarAvailable.get().getCarNo(), digitCarNo);
                    return registCarAvailable.get();
                }
                log.info("차량번호 = {}({}) : 숫자일치 목록중 완전 일치 강제 패턴에 등록되지 않은 차량 없음 ", carNo, digitCarNo);
                return null;
            }
        }
    }

    /**
     * 완전일치 강제 패턴 등록 여부
     *
     * @param carNo 차량번호
     * @return 등록 여부
     */
    private boolean hasExactCarNoPattern(String carNo) {
        List<LogicPattern> exactCarNos = logicPatternRepository.findLogicPatternByExactCarNo(carNo);
        if (exactCarNos.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * 앱 방문 차량
     *
     * @param carNo 차량번호
     * @return 앱 방문 차량
     */
    private AppVisitCar findAppVisitCar(String carNo) {
        LocalDateTime today = LocalDateTime.now();
        return appVisitCarRepository.findByVisitCarNoAndAccessPeriodBeginDtBeforeAndAccessPeriodEndDtAfter(carNo, today.plusHours(1), today.minusHours(1)).stream()
                .findFirst().orElse(null);
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
     * 출차제한 허용 여부
     * @param requestDto
     * @return
     */
    private boolean isRestrictedCar(InterlockRequestDto requestDto) {
        return visitCarRepository.findTopByCarNoAndLvvhclDtIsNullOrderByEntvhclDtDesc(requestDto.getCarNo())
                .map(visitCar -> {
                    if( requestDto.getGateType() == 3
                            && "Y".equals(requestDto.getLeaveCarRestrictionUseYn())
                            && StringUtils.contains(requestDto.getInstallOption(), "LPR_CAMERA2")
                            && StringUtils.contains(requestDto.getInstallOption(), "SIGNAGE")
                            && visitCar.getRestrictLeaveCar() == 0
                            && (visitCar.getCarSection() == 4
                            || visitCar.getCarSection() == 5)
                    ) {
                        log.info("차량번호 = {}, 통로 = {}({}) 출차 제한 차량, {} 출차 제한됨" ,requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId(), visitCar.getRestrictLeaveCar());
                        return true;
                    } else {
                        log.info("차량번호 = {}, 통로 = {}({}) 출차 제한 차량, {} 출차 허용됨" ,requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId(), visitCar.getRestrictLeaveCar());
                        return false;
                    }
                }).orElseGet(() -> false);
    }

    /**
     * 방문차량 주차시간 분으로 환산
     * @param localDate
     * @return
     */
    private long getAllowableTimeMinutes(Date localDate) {
        if (localDate == null) {
            return 0;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(localDate);
        calendar.add(Calendar.HOUR_OF_DAY, -9);
        Date date = calendar.getTime();

        int years = date.getYear();
        int days =  date.getDate();
        int hours =  date.getHours();
        int minutes =  date.getMinutes();

        if (years < 100) { // 이해는 안가지만 서기 2000년이 100임
            days = 0;
        }

        return (days * 24 * 60) + (hours * 60) + minutes;
    }

    /**
     * 방문차량 주차시간 글로벌 설정 여부
     * @param requestDto
     * @return
     */
    private boolean hasGlobalAllowableTime(InterlockRequestDto requestDto) {
        Date globalAllowableTime = requestDto.getVisitAllowableTime();
        long minutes = this.getAllowableTimeMinutes(globalAllowableTime);
        //글로벌 설정 값이 없거나 0이면 허용
        if (minutes == 0) {
            return false;
        }
        return true;
    }

    /**
     * 방문차량 주차시간 제한시간 이내 여부
     * @param requestDto
     * @return
     */
    private boolean inAllowableTime(InterlockRequestDto requestDto) {
        Date globalAllowableTime = requestDto.getVisitAllowableTime();
        //글로벌 설정 값이 없거나 0이면 허용
        if (!this.hasGlobalAllowableTime(requestDto)) {
            return true;
        }
        long globalMinutes = this.getAllowableTimeMinutes(globalAllowableTime);

        log.info("차량번호 = {}, 글로벌 설정 분:{}",requestDto.getCarNo(), globalMinutes);

        if (globalMinutes == 0) {
            return true;
        }

        return visitCarRepository.findTopByCarNoAndLvvhclDtIsNullOrderByEntvhclDtDesc(requestDto.getCarNo())
                .map(visitCar -> {
                    long visitCarAllowableTimeMinutes = this.getAllowableTimeMinutes(visitCar.getVisitAllowableTime());
                    //개별 설정 값이 없거나 0이면 글로벌 설정값을 따름
                    if (visitCarAllowableTimeMinutes == 0) {
                        log.info("차량번호 = {}, 방문차량 주차시간 개별 설정 값이 없음", requestDto.getCarNo());
                        //입차시간 + 글로벌 허용시간이 현재 시간 이후 이면 통과
                        if (visitCar.getEntvhclDt().plusMinutes(globalMinutes).isAfter(LocalDateTime.now()) ) {
                            return true;
                        }else{
                            return false;
                        }
                    }else{
                        //입차시간 + 개별 허용시간이 현재 시간 이후 이면 통과
                        log.info("차량번호 = {}, 개별 차량 설정 분:{}",requestDto.getCarNo(), visitCarAllowableTimeMinutes);

                        if (visitCar.getEntvhclDt().plusMinutes(visitCarAllowableTimeMinutes).isAfter(LocalDateTime.now()) ) {
                            return true;
                        }else{
                            return false;
                        }
                    }

                }).orElseGet(() -> true); //없거나 1개 이상일 경우 통과
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
//        boolean warningCar = warningCarRepository.existsByCarNo(carNo);
        boolean warningCar = warningCarRepository.findWarningCarByCarNoAndStatus(carNo).isPresent();
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
        log.info("차량번호 = {}, 통로 = {} 출입 허용",requestDto.getCarNo(), requestDto.getGateName());
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
        log.info("차량번호 = {}, 통로 = {} 출입 차단",requestDto.getCarNo(), requestDto.getGateName());
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
