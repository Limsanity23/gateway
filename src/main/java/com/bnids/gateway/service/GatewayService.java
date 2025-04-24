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
import com.bnids.gateway.dto.*;
import com.bnids.gateway.entity.*;
import com.bnids.gateway.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.mapping.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

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

    @NonNull
    private final SettingsRepository settingsRepository;

    @NonNull
    private final ReservationRepository reservationRepository;

    @NonNull
    private final AptnerService aptnerService;

    @NonNull
    private final ParkingDiscountService parkingDiscountService;

    private final List<AptnerReserve> reserveCarList = new ArrayList<>();

    private long reserveCarListLoadTime;

    private final long reserveCarListCacheDuration = 600 * 1000L; //10분마다 목록 갱신

    private boolean needToForward = false;
    private String forwardUrl;
    private HashMap<Long, Long> forwardGates = new HashMap<>();

    private SystemSetup systemSetup;

    @PostConstruct
    public void init(){
        log.info("* GatewayService init *");
        systemSetup = findSystemSetup();
        initAptner();
        initMemorySettings();
    }

    private void initAptner() {
        log.info("initAptner");
        SystemSetup system = findSystemSetup();
        try {
            log.info("* 사이트코드 : {}, 아파트너 연동여부 : {}",system.getSiteCode(), aptnerService.isAptner(system.getSiteCode()));
            if (aptnerService.isAptner(system.getSiteCode())) {
                List<AptnerReserve> list = aptnerService.getAptnerVisitAll(system.getSiteCode());
                if(list != null) {
                    log.info("* 아파트너 방문예약 건수 : {}",list.size());
                    reserveCarList.addAll(list);
                    log.info("reserveCarList empty 여부: {}", reserveCarList.isEmpty());

                    if ( reserveCarList != null) {
                        for (AptnerReserve rev : reserveCarList) {
                            log.info("* carNo: {}, dong: {}, ho: {}", rev.getCarNo(), rev.getDong(), rev.getHo());
                        }
                        reserveCarListLoadTime = System.currentTimeMillis();
                    }
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void initMemorySettings() {
        log.info("initMemorySettings");
        Optional<Settings> findJSONSettingToForward = settingsRepository.findJSONSettingToForward();
        needToForward = findJSONSettingToForward.isPresent();
        log.info("needToForward: {}", needToForward);
        if (!needToForward) {
            return;
        }
        /*
         JSON 구조 예시 : {
            "url":"https://bn211105.hparking.co.kr:9443/gateway/api/gateway/interlock",
            "gates": [ {"fromGateId": 1, "toGateId": 6},
                       {"fromGateId": 2, "toGateId": 7}]
         }
         */
        // 1. json 데이터를 파싱
        try {
            JSONParser parser = new JSONParser();
            JSONObject objData = (JSONObject)parser.parse(findJSONSettingToForward.get().getValue());
            forwardUrl = (String)objData.get("url");
            JSONArray arrData = (JSONArray)objData.get("gates");
            for (int i = 0; i < arrData.size(); i++) {
                JSONObject obj = (JSONObject)arrData.get(i);
                forwardGates.put((Long)obj.get("fromGateId"), (Long)obj.get("toGateId"));
            }

            // Now you can access the JSON data using jsonMap.
        } catch (ParseException e) {
            e.printStackTrace();
        }        
        // 2. url 추출
        // 3. gates 추출


    }

//    public boolean checkAptnerReserve(String siteCode, String carNo){
    public InterlockRequestDto checkAptnerReserve(String siteCode, InterlockRequestDto requestDto){
        log.info("** 아파트너 방문예약 등록여부 확인 시작 : {} - 동:{}. 호:{} *", requestDto.getCarNo(), requestDto.getAddressDong(), requestDto.getAddressHo());
        long now = System.currentTimeMillis();
        log.info("** now - reserveCarListLoadTime: {}, 목록유효시간: {}", now - reserveCarListLoadTime, reserveCarListCacheDuration);
        boolean isReserve = false;
        try {
            if (reserveCarList.isEmpty() || now - reserveCarListLoadTime > reserveCarListCacheDuration) {
                log.info("* reserveCarList 비어 있거나 유효시간이 지남 *");
                synchronized (reserveCarList) {
                    if (reserveCarList.isEmpty()  || now - reserveCarListLoadTime > reserveCarListCacheDuration) {
                        List<AptnerReserve> result = aptnerService.getAptnerVisitAll(siteCode);
                        log.info("# 아파트너 방문예약 result size: {}", result.size());
                        reserveCarList.clear();
                        reserveCarList.addAll(result);
                        if (reserveCarList != null) {
                            for (int i=0; i < reserveCarList.size(); i++) {
                                log.info("* carNo: {}", reserveCarList.get(i).getCarNo());
                                log.info("* dong: {}", reserveCarList.get(i).getDong());
                                log.info("* ho: {}", reserveCarList.get(i).getHo());
                            }
                        }
                        reserveCarListLoadTime = now;
                    }
                }
            }

            for (AptnerReserve item : reserveCarList) {
                if (item.getCarNo().equals(requestDto.getCarNo())) {
                    log.info("* {} 는 아파트너 방문예약 차량 *", requestDto.getCarNo());
                    isReserve = true;
                    //입차통보 api 호출
                    aptnerService.sendAccessIn(siteCode, item.getCarNo(), item.getDong(), item.getHo());
                    requestDto.setAddressDong(item.getDong());
                    requestDto.setAddressHo(item.getHo());
                    break;
                }
            }

            if (!isReserve) log.info("* {} 는 아파트너 방문예약 차량이 아님 *", requestDto.getCarNo());

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return requestDto;
    }

    public void interlock(LprRequestDto lprRequestDto) {
        Integer accuracy = lprRequestDto.getAccuracy();
        if (accuracy == null) accuracy = 0;
        Integer accuracy2 = lprRequestDto.getAccuracy2();
        if (accuracy2 == null) accuracy2 = 0;
        Long gateId = lprRequestDto.getGateId();
        String carNo1 = lprRequestDto.getLprCarNo();
        String carNo2 = lprRequestDto.getLprCarNo2();
        String carNo = "";
        String carImage = lprRequestDto.getCarImage();
        boolean bothHaveNumber = false;

        // If the gate is already opened by LPR, return true otherwise false
        boolean isGateAlreadyUp = lprRequestDto.isGateAlreadyUp();


        // 다른 현장으로 전달하는 경우인지 체크하고 전달하기
        this.checkSettingsFowardAndSend(lprRequestDto);


        log.info("@@ 1 carNo: {}, carNo1: {}, carNo2: {}", carNo, carNo1, carNo2);

        log.info("@@ 인식엔진에서 넘어온 데이터 조회 {} ", lprRequestDto.toString());

        log.info("@@ 2 carNo: {}, carNo1: {}, carNo2: {}", carNo, carNo1, carNo2);
        carNo1 = lprRequestDto.getLprCarNo();
        carNo2 = lprRequestDto.getLprCarNo2();
        log.info("@@ 3 carNo: {}, carNo1: {}, carNo2: {}", carNo, carNo1, carNo2);

        if (carNo1 == null && carNo2 == null) {
            log.info("@@ 인식엔진에서 넘어온 차번호 두개가 모두 null");
            return;
        }
        if (carNo1 == null) {
            carNo = "";
            log.info("@@ carNo1 == null");
        }else if (carNo2 == null) {
            carNo2 = "";
            log.info("@@ carNo2 == null");
        }

        // 차량번호가 4자리 미만으로 넘어온 경우 미인식으로 처리하기로 함 20220112
        if (carNo1.length() < 4) {
            accuracy = 0;
            carNo1 = "미인식";
        }
        if (carNo2.length() < 4) {
            accuracy2 = 0;
            carNo2 = "미인식";
        }

        if (!carNo1.startsWith("미인식") && carNo2.startsWith("미인식")){
            log.info("@@ carNo1 != 미인식 && carNo2 == 미인식");
            carNo = carNo1;
            carImage = lprRequestDto.getCarImage();
        } else if (carNo1.startsWith("미인식") && !carNo2.startsWith("미인식")) {
            log.info("@@ carNo1 == 미인식 && carNo2 != 미인식");
            carNo = carNo2;
            carImage = lprRequestDto.getCarImage2();
        }else if (accuracy > 0 && accuracy2 > 0) { //둘다 인식
            bothHaveNumber = true;
            carNo = carNo1;
        } else if (accuracy2 > 0) {
            carNo = carNo2;
            carImage = lprRequestDto.getCarImage2();
        } else if (accuracy > 0) {
            carNo = carNo1;
            carImage = lprRequestDto.getCarImage();
        } else { //둘다 미인식
            carNo = "미인식_";
        }

        log.info("@@ carNo: {}, carNo1: {}, carNo2: {}", carNo, carNo1, carNo2);

        SystemSetup systemSetup = findSystemSetup();
        Integer logicType = systemSetup.getLogicType();
        Integer operationLimitSetup = systemSetup.getOperationLimitSetup();
        String leaveCarRestrictionUseYn = systemSetup.getLeaveCarRestrictionUseYn();
        Date visitAllowableTime = systemSetup.getVisitAllowableTime();
        Gate gate = findGate(gateId);

        String gateName = gate.getGateName();

        Integer transitMode = gate.getTransitMode();

        log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) 출입 시작", carNo, carNo2, gateName, gateId);
        long beforeTime = System.currentTimeMillis();

        InterlockRequestDto requestDto = InterlockRequestDto.builder()
                .lprCarNo(carNo)
                .carNo(carNo)
                .gateId(gateId)
                .gateName(gate.getGateName())
                .gateType(gate.getGateType())
                .installOption(systemSetup.getInstallOption())
                .installDevice(gate.getInstallDevice())
                .carImage(carImage)
                .plateType(lprRequestDto.getPlateType())
                .leaveCarRestrictionUseYn(leaveCarRestrictionUseYn)
                .visitAllowableTime(visitAllowableTime)
                .paymentSuccess(lprRequestDto.isPaymentSuccess())
                .gatePaymentType(gate.getGatePaymentType())
                .transitMode(transitMode)
                .siteCode(systemSetup.getSiteCode())
                .note(lprRequestDto.getNote())
                .carImageColor(lprRequestDto.getCarImageColor())
                .build();

        RegistCar registCar = findRegistCar(carNo, logicType);

        // 결제 미인식이어도 보내야 한다. 결제의 경우 미인식 ---- 미인식

        // 결제를 사용중이고 출구인 경우
        // 결제를 보낸다.
        if ("Y".equals(systemSetup.getPaymentEnabledYn()) && gate.getGateType() > 2 && transitMode == 4) {
            // 결제 후 통과 시작
            if (gate.getGatePaymentType() == 1) {
                Optional<UnmannedPaymentKiosk> unmannedPaymentKiosk = unmannedPaymentKioskRepository.findByGateId(requestDto.getGateId());
                unmannedPaymentKiosk.ifPresent(
                        paymentKiosk -> {
                            requestDto.setUnmannedPaymentKioskId(paymentKiosk.getId());
                        }
                );
            }


            if (registCar == null) {
                // 출입 차단
                if (StringUtils.contains(carNo, "미인식")) {
                    requestDto.setCarSection(1L);
                    interlockService.sendSignageServer(requestDto);
                } else {
//                    boolean isWarningCar = isWarningCar(carNo, requestDto, false);
                    boolean isWarningCar = isWarningCarForPayment(carNo, requestDto, false);
                    log.info("* 결제통과 통로에서 경고차량 여부 = {}", isWarningCar);
                    if (isWarningCar) { // 경고 차량
                        requestDto.setCarSection(6L);
                    } else {
                        //앱 방문차량인 경우 로컬에서 게이트웨이에서 보내준 파라미터의 carSection을 기준으로 푸시전송여부를 판별하므로
                        //여기서 앱방문차량인지 확인 후 carSection 셋팅하도록 한다
//                    requestDto.setCarSection(2L);
                        AppVisitCar appVisitCar = this.findAppVisitCar(carNo);
                        Reservation reservation = this.findReservationCar(carNo);
                        if (appVisitCar == null && reservation == null) {
                            if(getEmergenyType(carNo))  requestDto.setCarSection(13L);
                            else requestDto.setCarSection(2L);
                        } else {
//                        requestDto.setCarSection(3L);\
                            if (appVisitCar != null) {
                                requestDto.setBy(appVisitCar);
                            }
                            if (reservation != null) {
                                requestDto.setByReservation(reservation);
                            }

                        }
                        log.info("차량번호: {},  미등록 차량, carSection : {}", requestDto.getCarNo(), requestDto.getCarSection());
                        this.processAfterPayment(requestDto, isGateAlreadyUp);
                    }
                }

            } else {
                requestDto.setBy(registCar);
                log.info("차량번호: {},  isPaymentSuccess : {}", requestDto.getCarNo(), requestDto.isPaymentSuccess());
                this.processAfterPayment(requestDto, isGateAlreadyUp);
//                if (requestDto.getGatePaymentType() == 2) { //210927 cks 유인정산의 경우 차단기 열어줌 - 로컬서버에 입주자 차량인 경우 자동으로 차단기를 열어주는 로직이 없음.
//                    accessAllowed(requestDto, isGateAlreadyUp);
//                }
            }

        } else if (StringUtils.contains(carNo, "미인식")) {
            String[] sl = carNo.split("_");
            if (sl != null ) {
                if (sl.length == 1)
                    requestDto.setCarNo(carNo+"_"+System.currentTimeMillis());
                else if (sl.length > 1)
                    requestDto.setCarNo(sl[1]+"_"+System.currentTimeMillis()); //20211123 cks 인식엔진에서 lprCarNo가 '미인식_xx'으로 넘어온 경우 미인식 유형별로 (ex사람, 오토바이..) 저장하도록 변경.
            }
            else requestDto.setCarNo("미인식_"+System.currentTimeMillis());
            log.info("# 저장용 미인식 carNo : {}", requestDto.getCarNo());

            requestDto.setCarSection(1L);
            if (transitMode == 3) {
                // 무조건 통과인 경우만 출입 허용
                accessAllowed(requestDto, isGateAlreadyUp);
            } else {
                accessBlocked(requestDto);
            }
        } else { // 인식, 오인식

            if (bothHaveNumber && !carNo.equals(carNo2)) { //두개 다 번호가 있지만, 그 두 번호가 같지 않은 경우에
                if (registCar == null) {
                    log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) {}가 검색되지 않음", carNo, carNo2, gateName, gateId, carNo);

                    RegistCar registCar2 = findRegistCar(carNo2, logicType);
                    // 두 번호 모두 등록되지 않은 경우 차량 번호의 길이가 지나치게 짧거나(5자리 이하) 숫자로 시작하지 않으면 무시
                    if (registCar2 == null && (carNo2.length() <= 5 || !Character.isDigit(carNo2.charAt(0)))) { //1110 or 울산53사1110
                        log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) {}도 검색되지 않음", carNo, carNo2, gateName, gateId, carNo2);
                        log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) {}은 패턴이 보편적이지 않아 선택하지 않음", carNo, carNo2, gateName, gateId, carNo2);
                    }else {
                        log.info("차량번호1 = {}, 차량번호2 = {}, 통로 = {}({}) {}를 선택함", carNo, carNo2, gateName, gateId, carNo2);
                        registCar = registCar2;
                        carNo = carNo2;
                        requestDto.setLprCarNo(carNo);
                        requestDto.setCarNo(carNo);
                        requestDto.setCarImage(lprRequestDto.getCarImage2());
                        requestDto.setCarImageColor(lprRequestDto.getCarImageColor2()); //cks 추가
                    }
                }
            }

            // boolean isWarningCar = isWarningCar(carNo, requestDto);
            if (transitMode == 1) { // 획인후 통과
                boolean isAllowPass = true;

                if (registCar == null) {
                    // 에약 방문 차량 조회
                    if (aptnerService.isAptner(systemSetup.getSiteCode()) && checkAptnerReserve(systemSetup.getSiteCode(), requestDto).getAddressDong() != null) { //아파트너 연동 현장이면
                        requestDto.setCarSection(3L);
                        log.info("% 아파트너 연동 현장 - 아파트너 방문예약 차량:{}, 등록항목:{}, 동-호: {} - {} -> 통과 %", carNo, requestDto.getCarSection(), requestDto.getAddressHo(), requestDto.getAddressHo());
                        accessAllowed(requestDto, isGateAlreadyUp);
                    } else {
                        AppVisitCar appVisitCar = this.findAppVisitCar(carNo);
                        Reservation reservation = this.findReservationCar(carNo);
//                        log.info("* AppVisitCarId: {},  ReservationId: {} ", appVisitCar.getAppVisitCarId(), reservation.getReservationId());
                        if (appVisitCar == null && reservation == null) {
                            log.info("* AppVisitCar 와 Reservation 모두 내역이 없음");
                            // 오인식 된 번호판 정보 => 부분일치, 임시로직 에 부합되는 등록 차량인지 판별, visit_car에도 기록
                            long taxiType = getTaxiType(carNo);
                            boolean isEmergencyType = getEmergenyType(carNo);
                            log.info("* taxiType:{}, isEmergencyType: {}", taxiType, isEmergencyType);
                            if (taxiType > 0) {
                                long lastCarSection = getLastCarSection(requestDto, Long.valueOf(taxiType).intValue()).longValue();
                                log.info("* getLastCarSection: {}", lastCarSection);
                                //화물차량이 키오스크를 누르고 입차하는 경우(키오스크 세대방분) 출차할 때에도 입차시 carSection을 유지할 수 있도록 처리
                                requestDto.setCarSection(lastCarSection);
                            } else if (isEmergencyType){
                                requestDto.setCarSection(13L);
                            } else {
                                List<LogicPattern> logicPatterns = logicPatternRepository.findLogicPatternBycarNo(carNo);
                                if (logicPatterns.size() == 0) {
                                    log.info("차량번호 = {}, 통로 = {}({}) 모든 개별로직에 부합하지 않음", carNo, gateName, gateId);
                                    // 출차인 경우 입차 기록을 찾아서 carsection을 기록함
                                    // 없으면 일반방문차량
                                    // 입출차 기록이 안맞는 데이터의 차량이 입차시 기존의 carsection을 가지고 오는 오류 수정
                                    requestDto.setCarSection(getLastCarSection(requestDto, 2).longValue());
                                } else {
                                    final LogicPattern logicPattern = logicPatterns.get(0);
                                    log.info("차량번호 = {}, 통로 = {}({}) 이 번호와 관련된 개별로직 갯수 {}", carNo, gateName, gateId, logicPatterns.size());
                                    registCar = findRegistCar(logicPattern.getRegistCarId());
                                    log.info("차량번호 = {}, 통로 = {}({}) LogicPattern: {}, 이 패턴으로 찾은 첫번째 차량번호: {}", carNo, gateName, gateId, logicPattern.getLogicPattern(), registCar.getCarNo());
                                    requestDto.setBy(registCar);
                                }
                            }
                        } else {
                            if (appVisitCar != null) {
                                log.info("* appVisitCar set");
                                requestDto.setBy(appVisitCar);
                            } else if (reservation != null) {
                                log.info("* reservation set");
                                requestDto.setByReservation(reservation);
                            }

                        }
                    }
                } else { //registCar != null
                    log.info("@ 차량번호 = {}(등록차량) requestDto 찍기: {}", carNo, requestDto.toString());
                    requestDto.setBy(registCar);

                    if(isRestrictedCar(requestDto)) {
                        isAllowPass = false;
                    }
                }
                log.info("* ({})경고차량 여부 체크 전 carsection 확인: {}", requestDto.getCarNo(), requestDto.getCarSection());
                boolean isWarningCar = isWarningCar(carNo, requestDto, registCar != null);
                log.info("* 경고차량 여부 체크 = {}, carsection = {}", isWarningCar, requestDto.getCarSection());
                if (isWarningCar) { // 경고 차량
                    requestDto.setCarSection(6L);
                }

                //내부입차일 경우 이전 통로에서의 항목이 유지되도록 해야한다
                if (requestDto.getGateType() == 2) { //내부입차
                    Optional<VisitCar> visitCar = visitCarRepository.findTopByCarNoAndLvvhclDtIsNullOrderByEntvhclDtDesc(requestDto.getCarNo());
                    if ( visitCar.isPresent() ) {
                        log.info("* 내부입차 visitCar에서 조회된 이전 입차의 카섹션: {}", visitCar.get().getCarSection());
                        requestDto.setCarSection(Long.valueOf(visitCar.get().getCarSection()));
                    }
                }

                log.info("* 차량번호 = {}, req차량번호 = {}, 카섹션: {} 통로 = {}({}) isAllowPass: {}",carNo, requestDto.getCarNo(), requestDto.getCarSection(), gateName, gateId, isAllowPass);

                isAllowPass = isAllowPass && isAllowPass(requestDto, transitMode, operationLimitSetup);
                log.info("* 차량번호 = {}, , req차량번호 = {}, 카섹션: {} 통로 = {}({}) isAllowPass2: {}",carNo, requestDto.getCarNo(), requestDto.getCarSection(), gateName, gateId, isAllowPass);

                if (isAllowPass) {
                    log.info("제한된 차량 조회 carSection1: {}",requestDto.getCarSection());
                    String restrictedMessage = isCustomRestricted(requestDto);
                    if (!"".equals(restrictedMessage)) {
                        isAllowPass = false;
                    }

                }

                if (isAllowPass) {
                    // 출입허용
                    accessAllowed(requestDto, isGateAlreadyUp);
                } else {
                    // 출입 차단

                    // 출구인 경우 방문차량 주차시간 설정에 따른 예외 허용
                    if (gate.getGateType() == 3 && this.hasGlobalAllowableTime(requestDto)) { //글로벌 설정이 있는 상태에서
                        log.info("차량번호 = {}, 통로 = {}({}) 방문차량 주차시간 글로벌 설정 있음", carNo, gateName, gateId);
                        boolean isAllowableTime = inAllowableTime(requestDto);
                        log.info("* 차량번호 = {} inAllowableTime = {}", carNo, isAllowableTime);
//                        if (inAllowableTime(requestDto)) { // 제한시간 이내이면 허용
                        if (isAllowableTime) { // 제한시간 이내이면 허용
                            accessAllowed(requestDto, isGateAlreadyUp);
                        } else { // 아니면 전광판에 표시
                            requestDto.setCarSection(100L); //주차시간초과 차량
                            accessBlocked(requestDto);
                        }
                    } else {
                        log.info("@ 출입차단 > 차량번호(carNo): {}, 리퀘스트 차량번호: {}", carNo, requestDto.getCarNo());
                        log.info("@ 출입차단 request 찍기: ", requestDto.toString());
                        accessBlocked(requestDto);
                    }
                }

            } else {

                if (registCar == null) {
                    AppVisitCar appVisitCar = findAppVisitCar(carNo);

                    if (appVisitCar == null) {
                        Reservation reservation = findReservationCar(carNo);
                        if (reservation == null) {
                            requestDto.setCarSection(2L);
                        } else {
                            requestDto.setCarSection(3L);
                            requestDto.setByReservation(reservation);
                        }
                    } else {
                        requestDto.setCarSection(3L);
                        requestDto.setBy(appVisitCar);
                    }
                } else {
                    requestDto.setBy(registCar);
                }

                requestDto.setCarSection(getLastCarSection(requestDto, requestDto.getCarSection().intValue()).longValue());

                if (transitMode == 2) { // 인식후 통과
                    boolean isOperationLimit = isCarAccessLimit(requestDto, transitMode, operationLimitSetup);
                    boolean isWarningCar = isWarningCar(carNo, requestDto, registCar != null);
                    if (!isOperationLimit && isWarningCar) {
                        requestDto.setCarSection(6L);
                        accessBlocked(requestDto);
                    } else {
                        //20220512 입주자 차량 중 택시가 있을 때 registCar 에 값이 있어도 TaxiType에 해당되면 carSection을 영업용 차량으로 덮어쓰는 현상이 있어
                        //registCar가 null 일 경우에만 TaxiType을 체크하도록 수정
                        if (registCar == null) {
                            long taxiType = getTaxiType(carNo);
                            if (taxiType > 0) {
                                requestDto.setCarSection(getLastCarSection(requestDto, (int) taxiType).longValue());
                            }

                        }
                        if(getEmergenyType(carNo))  requestDto.setCarSection(13L);
                        accessAllowed(requestDto, isGateAlreadyUp);
                    }
                } else if (transitMode == 3) { // 무조건 통과
                    if(getEmergenyType(carNo))  requestDto.setCarSection(13L);
                    accessAllowed(requestDto, isGateAlreadyUp);
                } else {
                    if(isWarningCar(carNo, requestDto, registCar != null)) {
                        accessBlocked(requestDto);
                    } else {
                        accessAllowed(requestDto, isGateAlreadyUp);
                    }
                }
            }
        }

        // 다른 모드인 경우를 구별해서 담아줄 필요가 있다.
        long afterTime = System.currentTimeMillis();
        long elapseTime  = afterTime - beforeTime;

        // 결제 후 통과가 아니여도 정산을 사용하면 요금계산은 되어야 한다.
        if (elapseTime > 1000) {
            log.info("Lazy Log : 차량번호 = {} {} ms", requestDto.getCarNo(), elapseTime);
        }
    }

    private boolean isWarningCarForPayment(String carNo, InterlockRequestDto requestDto, boolean b) {
        List<WarningCar> warningCarList = warningCarRepository.findWarningCarByCarNoAndStatus(carNo);
        return warningCarList.size() > 0;
    }

    public void registAutoWarningCar(InterlockRequestDto requestDto, WarningCarAutoRegistRulesDto warningCarAutoRegistRulesDto) {
        log.info("# 경고차량 자동등록 처리 > {}({}), 정책: {} #", requestDto.getCarNo(), warningCarAutoRegistRulesDto.getCarSection(), warningCarAutoRegistRulesDto.getWarinigCarRulesSection());
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


    public void processAfterPayment(InterlockRequestDto requestDto, boolean isGateAlreadyUp) {
        log.info("# processAfterPayment > isPaymentSuccess: {}, 카섹션: {}", requestDto.isPaymentSuccess(), requestDto.getCarSection());
        if (requestDto.isPaymentSuccess() || isGateAlreadyUp) {
            if ( requestDto.getCarSection() == 6 ) {
                log.info("# processAfterPayment > isPaymentSuccess는 true 이나 대상차량이 경고차량임 #");
                accessBlocked(requestDto);
            } else {
                accessAllowed(requestDto, isGateAlreadyUp);
            }
        } else {
            if(requestDto.getGatePaymentType() == 1) {
                interlockService.sendUnmannedPaymentServer(requestDto);
            } else {
                interlockService.sendMannedPaymentServer(requestDto);
            }
            log.info("*** requestDto : {}", requestDto.toString());
            interlockService.sendSignageServer(requestDto);
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
     * @return 등록차량ExpoPushService.java
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

        if(digitCarNo.length() < 4) {
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
     * 앱 방문 차량 (새 방문예약)
     *
     * @param carNo 차량번호
     * @return 앱 방문 차량
     */
    /*private Reservation findReservationCar(String carNo) {
        List<Settings> timeList = settingsRepository.findEntryExitBufferTime();
        int entryBufferTime = 60; // 방문예약차량 입차 시 기본 여유시간
        int exitBufferTime = 60; // 방문예약차량 출차 시 기본 여유시간

        if (timeList.size() >= 2) {
            entryBufferTime = Integer.parseInt(timeList.get(0).getValue());
            exitBufferTime = Integer.parseInt(timeList.get(1).getValue());
        }

        LocalDateTime today = LocalDateTime.now();
        LocalDateTime accessPeriodBeginDt = today.plusMinutes(entryBufferTime);
        LocalDateTime accessPeriodEndDt = today.minusMinutes(exitBufferTime);

        return reservationRepository.findByVisitCarNoAndAccessPeriodBeginDtBeforeAndAccessPeriodEndDtAfter(
                carNo, accessPeriodBeginDt, accessPeriodEndDt
        ).stream().findFirst().orElse(null);
    }*/

    private Reservation findReservationCar(String carNo) {
        log.info("### 방문예약 차량 조회 시작: {}", carNo);

        List<Settings> timeList = settingsRepository.findEntryExitBufferTime();
        int entryBufferTime = 60; // 방문예약차량 입차 시 기본 여유시간
        int exitBufferTime = 60; // 방문예약차량 출차 시 기본 여유시간

        if (timeList.size() >= 2) {
            entryBufferTime = Integer.parseInt(timeList.get(0).getValue());
            exitBufferTime = Integer.parseInt(timeList.get(1).getValue());
        }

        log.info("### 버퍼 시간 설정: 입차={}, 출차={}", entryBufferTime, exitBufferTime);

        LocalDateTime today = LocalDateTime.now();
        LocalDateTime accessPeriodBeginDt = today.plusMinutes(entryBufferTime);
        LocalDateTime accessPeriodEndDt = today.minusMinutes(exitBufferTime);

        log.info("### 계산된 접근 허용 시간 범위: {} - {}", accessPeriodBeginDt, accessPeriodEndDt);

        // 모든 유효한 예약 조회 (시간 미지정 + 일반 예약)
        List<Reservation> allReservations = reservationRepository.findByVisitCarNoAndAccessPeriodBeginDtBeforeAndAccessPeriodEndDtAfter(
                carNo, accessPeriodBeginDt, accessPeriodEndDt);

        log.info("### 모든 예약 조회 결과: {} 건", allReservations.size());

        // 예약들 중에서 visit_car_id가 null인 시간 미지정 예약을 우선적으로 찾음
        LocalDateTime unlimitedStartDate = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        LocalDateTime unlimitedEndDate = LocalDateTime.of(2100, 1, 1, 0, 0, 0);

        Optional<Reservation> unusedUnlimitedReservation = allReservations.stream()
                .filter(r -> r.getAccessPeriodBeginDt().equals(unlimitedStartDate) &&
                        r.getAccessPeriodEndDt().equals(unlimitedEndDate) &&
                        r.getVisitCarId() == null)
                .findFirst();

        if (unusedUnlimitedReservation.isPresent()) {
            Reservation res = unusedUnlimitedReservation.get();
            log.info("### 사용 가능한 시간 미지정 예약 발견: id={}", res.getReservationId());
            return res;
        }

        // 시간 미지정 예약이 없는 경우, 일반 시간 지정 예약 중에서 찾음
        Optional<Reservation> normalReservation = allReservations.stream()
                .filter(r -> !(r.getAccessPeriodBeginDt().equals(unlimitedStartDate) &&
                        r.getAccessPeriodEndDt().equals(unlimitedEndDate)))
                .findFirst();

        if (normalReservation.isPresent()) {
            Reservation res = normalReservation.get();
            log.info("### 일반 시간 지정 예약 발견: id={}, visit_car_id={}", res.getReservationId(), res.getVisitCarId());
            return res;
        }

        log.info("### 차량 {} 에 대한 유효한 예약 없음", carNo);
        return null;
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
        log.info("@ 출차제한 허용 여부 조회 > request 차량번호: {}", requestDto.getCarNo());
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

        log.info("# hasGlobalAllowableTime(방문차량 주차시간 글로벌 설정 여부) globalAllowableTime: {}, minutes: {}", globalAllowableTime, minutes);

        //글로벌 설정 값이 없거나 0이면 허용 -> 아래 주석의 내용으로 정책변경
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
        //글로벌 설정 값이 없거나 0이면 허용 ->20210802 주석의 내용으로 정책변경
        if (!this.hasGlobalAllowableTime(requestDto)) {
            return true;
        }
        long globalMinutes = this.getAllowableTimeMinutes(globalAllowableTime);

        log.info("차량번호 = {}, 글로벌 설정 분:{}",requestDto.getCarNo(), globalMinutes);

        if (globalMinutes == 0) {
            return true;
        }

//        return visitCarRepository.findTopByCarNoAndLvvhclDtIsNullOrderByEntvhclDtDesc(requestDto.getCarNo()) //cks 20220802 아래 로직으로 테스트중... 여러번 출차시도를 하는 차량의 경우 출차시각을 계속 업데이트 하기로 했기 때문...
        return visitCarRepository.findTopByCarNoOrderByEntvhclDtDesc(requestDto.getCarNo())
                .map(visitCar -> {
                    long visitCarAllowableTimeMinutes = this.getAllowableTimeMinutes(visitCar.getVisitAllowableTime());
                    log.info("#차량번호 = {}, visitCarAllowableTimeMinutes:{}, carSection: {}",requestDto.getCarNo(), visitCarAllowableTimeMinutes, visitCar.getCarSection());
                    //개별 설정 값이 없거나 0이면 글로벌 설정값을 따름
                    if (visitCarAllowableTimeMinutes == 0) {
                        log.info("#차량번호 = {}, 방문차량 주차시간 개별 설정 값이 없음", requestDto.getCarNo());
                        //입차시간 + 글로벌 허용시간이 현재 시간 이후 이면 통과
                        if (visitCar.getEntvhclDt().plusMinutes(globalMinutes).isAfter(LocalDateTime.now()) ) {
                            log.info("#차량번호 = {}, 입차시간 + 글로벌 허용시간이 현재 시간 이후");
                            return true;
                        }else{
//                            return false; //주석처리
                            //20220419 cks [210607_다산 신안인스빌 퍼스트포레]의 사례로 테스트 중
                            //화물차량이 가상으로 등록된 동호에 키오스크 세대방문(카섹션:4)으로 출입하는 경우
                            //통로별 통과차량에 화물차량이 '아니오'로 등록, 글로벌 설정시간이 있고  출차 시 제한 시간을 초과했을 때
                            //키오스크 세대방문으로 들어왔더라도 위의 2가지 케이스에 걸려 출차가 안됨... 보완하여 테스트 중
                            if (visitCar.getCarSection() == 4) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }else{
                        //입차시간 + 개별 허용시간이 현재 시간 이후 이면 통과
                        log.info("#차량번호 = {}, 개별 차량 설정 분:{}",requestDto.getCarNo(), visitCarAllowableTimeMinutes);

                        if (visitCar.getEntvhclDt().plusMinutes(visitCarAllowableTimeMinutes).isAfter(LocalDateTime.now()) ) {
                            log.info("#차량번호 = {}, 입차시간 + 개별 허용시간이 현재 시간 이후");
                            return true;
                        }else{
                            return false;
                        }
                    }

//                }).orElseGet(() -> false); //없거나 1개 이상일 경우 통과
                }).orElseGet(() -> true); //없거나 1개 이상일 경우 통과 - 20220621 cks 통과처리 하려면 true로 반환되어야 함
    }

    /**
     * 입차시 등록항목 가져오기
     * @param requestDto
     * @param defValue
     * @return
     */
    private Integer getLastCarSection(InterlockRequestDto requestDto, Integer defValue) {
        if (requestDto.getGateType() == 3 || requestDto.getGateType() == 4) {
            return visitCarRepository.findTopByCarNoAndLvvhclDtIsNullOrderByEntvhclDtDesc(requestDto.getCarNo())
                    .map(VisitCar::getCarSection).orElse(defValue); //없거나 1개 이상일 경우
        }
        return defValue;
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

                        // 제한시간.0
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
     * 현장별 입차제한 로직 적용
     *
     * @param requestDto 연동요청 Dto
     * @return 차량 출입 제한 여부
     */
    public String isCustomRestricted(InterlockRequestDto requestDto) {
        log.info("차량번호 = {}, 통로 = {}({}), {}동 {}호 현장별 입차제한 로직 적용 시작",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId(), requestDto.getAddressDong(), requestDto.getAddressHo());

        String restrictedMessage = "";
        List<Settings> settings = settingsRepository.findCustomRestrictLogicList();
        if (settings == null || settings.size() == 0) return restrictedMessage;

        for (Settings setting : settings) {
            log.info("차량번호 = {}, 통로 = {}({}) {}:{}",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId(), setting.getName(), setting.getValue());
            if(setting.getId() == 1001L) { //세대방문 차량 월(1일~말일) 허용 주차시간 제한
                double parkingHours = visitCarRepository.getSumVisitCar45ParkingHours(requestDto.getAddressDong(), requestDto.getAddressHo());
                log.info("차량번호 = {}, 통로 = {}({}), {}동 {}호 주차시간 합계: {}",requestDto.getCarNo(),requestDto.getGateName(), requestDto.getGateId(), requestDto.getAddressDong(), requestDto.getAddressHo(), parkingHours);
                log.info("제한된 차량 조회 carSection2: {}",requestDto.getCarSection());
                if (requestDto.getCarSection() == 3L || requestDto.getCarSection() >= 10L) { // 예약방문 차량 혹은 입주자차량 이후 (직원차량...) 면 제한 예외.
                    log.info("차량번호 = {}, 통로 = {}({}) 제한 예외 - carSection: {}",requestDto.getCarNo(), requestDto.getGateName(), requestDto.getGateId(), requestDto.getCarSection());
                }else if (parkingHours > Double.parseDouble(setting.getValue())) {
                    requestDto.setCarSection(100L); //주차시간초과 차량
                    log.info("차량번호 = {} 세대방문 월허용 주차시간 초과",requestDto.getCarNo());
                    restrictedMessage = "세대방문 차량의\n 월허용 주차시간 제한 초과입니다.\n 관리실에 문의하세요.";
                }

            }
        }
        if ("".equals(restrictedMessage))
            log.info("차량번호 = {}, 통로 = {}({}) 현장별 입차제한 없음",requestDto.getCarNo(), requestDto.getGateName(), requestDto.getGateId());
        else
            log.info("차량번호 = {}, 통로 = {}({}) 현장별 입차제한 : {}",requestDto.getCarNo(), requestDto.getGateName(), requestDto.getGateId(), restrictedMessage);

        return restrictedMessage;

    }

    /**
     * 경고차량 여부
     *
     * @param carNo 차량번호
     * @return 경고차량 여부
     */
    private boolean isWarningCar(String carNo, InterlockRequestDto requestDto, boolean isRegistCar) {
        // 경고차량 삭제 차량인지 여부 확인
        // 결제 후 통과와 결제 후 통과가 아닌 경우를 생각해 보면
        // 단순하게 여기서 생각할건 넘겨야할

        // 삭제된 경고차량 여부
        List<WarningCar> warningCars = warningCarRepository.findWarningCarByCarNo(carNo);
        WarningCar warningCar = warningCars.size() > 0 ? warningCars.get(0) : null;
        if(warningCar != null && warningCar.getRegistStatus() == 1) {
            requestDto.setWarningCarDeleteDt(warningCar.getDeletedDt());
        }
        WarningCarAutoRegistRulesDto autoRegistWarningCarRulesDto = this.evaluateWarningCarRules(requestDto, isRegistCar);

        if(autoRegistWarningCarRulesDto != null) {
            registAutoWarningCar(requestDto, autoRegistWarningCarRulesDto);
        }
        //        boolean warningCar = warningCarRepository.existsByCarNo(carNo);
        boolean warningCarResult = warningCarRepository.findWarningCarByCarNoAndStatus(carNo).size() > 0;
        log.info("차량번호 = {} , 위반여부조회 결과: {}",carNo,  warningCarResult);
        if (warningCarResult) {
            log.info("차량번호 = {} 경고 차량차량으로 출입 차단됨",carNo);
        }
        return warningCarResult;
    }

    /**
     * 출입 허용
     *
     * @param requestDto 연동요청 Dto
     */
    private void accessAllowed(InterlockRequestDto requestDto, boolean isGateAlreadyUp) {
        log.info("NOTE : {}", requestDto.getNote());
        log.info("차량번호 = {}, 통로 = {} 출입 허용",requestDto.getCarNo(), requestDto.getGateName());
        requestDto.setGateStatus(1);

        if (!isGateAlreadyUp) { // only open the gate when it is not up yet
            interlockService.sendGateServer(requestDto);
        }
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
            int region = NumberUtils.toInt(StringUtils.left(StringUtils.right(carNo,7),2), 0);

            if (region > 0 && region < 70) {
                taxiType = 7L;
            } else if (region >= 70 && region < 80) {
                taxiType = 8L;
            } else if (region >= 80 && region <= 99) {
                taxiType = 9L;
            }
        }

        return taxiType;
    }

    private String digitCarNo(String carNo) {
        return carNo.replaceAll("[^0-9]", "");
    }



    private boolean getEmergenyType(String carNo) {
        boolean isEmergencyType = false;
        int region = NumberUtils.toInt(StringUtils.left(StringUtils.right(carNo,8),3), 0);
        if (region == 998 || region == 999) {
            isEmergencyType = true;
        }
        return isEmergencyType;
    }


    public WarningCarAutoRegistRulesDto evaluateWarningCarRules(InterlockRequestDto requestDto, boolean isRegistCar) {
        log.info("[경고차량 판단 시작] 차량번호: {}, 통로번호: {}", requestDto.getCarNo(), requestDto.getGateId());

        boolean isPaymentEnabled = systemSetup != null &&
                systemSetup.getPaymentEnabledYn() != null &&
                "Y".equals(systemSetup.getPaymentEnabledYn());
        log.info("[경고차량 판단 - 유료현장여부] 유료여부: {}", isPaymentEnabled ? "사용" : "미사용");

        List<WarningCarAutoRegistRules> rules = warningCarAutoRegistRulesRepository.findRules();
        Optional<Settings> inoutExcludeSettings = settingsRepository.findExcludeInternalInOut();
        log.info("[경고차량 판단 - 내부입출차설정] 내부입출차제외: {}",
                inoutExcludeSettings.isPresent() && "Y".equals(inoutExcludeSettings.get().getValue()) ? "사용" : "미사용");

        for(WarningCarAutoRegistRules rule : rules) {
            log.info("[경고차량 판단 - 정책 체크 시작 {}]", rule.getWarinigCarRulesSection());
            switch (rule.getWarinigCarRulesSection()) {
                case PARKING_DURATION_VIOLATION:
                    log.info("[경고차량 판단 - 정책확인] 정책ID: {}, 대상 차량유형: {}, 위반유형: 주차시간 위반",
                            rule.getWarningCarAutoRegistRulesId(),
                            rule.getCarSection());
                    break;

                case NUMBER_ACCESS_VIOLATION:
                    log.info("[경고차량 판단 - 정책확인] 정책ID: {}, 대상 차량유형: {}, 위반유형: 출입회수 위반",
                            rule.getWarningCarAutoRegistRulesId(),
                            rule.getCarSection());
                    break;

                default:
                    log.warn("[경고차량 판단 - 정책확인] 정책ID: {}, 대상 차량유형: {}, 위반유형: 알 수 없음",
                            rule.getWarningCarAutoRegistRulesId(),
                            rule.getCarSection());
                    break;
            }


            WarningCarAutoRegistRulesDto warningCarAutoRegistRules = WarningCarAutoRegistRulesDto
                    .builder()
                    .build()
                    .of(rule);
            warningCarAutoRegistRules.setCarNo(requestDto.getCarNo());

            if (isRegistCar && requestDto.getCarSection() != rule.getCarSection()) {
                log.info("[경고차량 판단 - 등록차량 스킵] 차량번호: {}, 요청 차량유형: {}, 정책 차량유형: {}",
                        requestDto.getCarNo(), requestDto.getCarSection(), rule.getCarSection());
                return null;
            }

            if(requestDto.getWarningCarDeleteDt() != null) {
                warningCarAutoRegistRules.setDeletedDt(requestDto.getWarningCarDeleteDt());
                log.info("[경고차량 판단 - 삭제이력] 차량번호: {}, 삭제일시: {}",
                        requestDto.getCarNo(), requestDto.getWarningCarDeleteDt());
            }

            List<VisitCarDto> visitCarList = visitCarRepositorySupport.findVisitCarListForRegistWarningCar(warningCarAutoRegistRules, isRegistCar);
            log.info("[경고차량 판단 - 입출차기록] 차량번호: {}, 조회된 입출차 수: {}",
                    requestDto.getCarNo(), visitCarList.size());
            int violationCount = calculateViolationCount(visitCarList, isPaymentEnabled, inoutExcludeSettings, rule);
            log.info("[경고차량 판단 - 위반집계] 차량번호: {}, 총 위반횟수: {}", requestDto.getCarNo(), violationCount);

            if (rule.getWarinigCarRulesSection() == WarningCarRegistEnum.NUMBER_ACCESS_VIOLATION) {
                log.info("[경고차량 판단 - 횟수위반체크] 차량번호: {}, 게이트타입: {}", requestDto.getCarNo(), requestDto.getGateType());
                if (requestDto.getGateType() == 1 || requestDto.getGateType() == 2) {
                    WarningCarAutoRegistRulesDto result = processViolationResult(warningCarAutoRegistRules, violationCount, rule, requestDto.getCarNo());
                    if (result != null) {
                        return result;  // 위반인 경우만 리턴
                    }
                    // 위반이 아닌 경우 다음 정책 체크를 위해 continue
                }
            } else {
                WarningCarAutoRegistRulesDto result = processViolationResult(warningCarAutoRegistRules, violationCount, rule, requestDto.getCarNo());
                if (result != null) {
                    return result;  // 위반인 경우만 리턴
                }
                // 위반이 아닌 경우 다음 정책 체크를 위해 continue
            }
        }
        return null;
    }

    private int calculateViolationCount(List<VisitCarDto> visitCarList, boolean isPaymentEnabled,
                                        Optional<Settings> inoutExcludeSettings, WarningCarAutoRegistRules rule) {
        int violationCount = 0;
        boolean isInoutExclude = inoutExcludeSettings.isPresent() &&
                inoutExcludeSettings.get().getValue().equals("Y");

        log.info("[위반횟수 계산 시작] 입출차기록 수: {}, 결제기능: {}, 내부입출차제외: {}",
                visitCarList.size(), isPaymentEnabled ? "사용" : "미사용", isInoutExclude ? "사용" : "미사용");
        for (VisitCarDto visitCar : visitCarList) {
            if (visitCar.getLvvhclDt() == null) {
                log.info("[위반횟수 계산 - 출차미완료] 차량번호: {}, 입차시간: {}",
                        visitCar.getCarNo(), visitCar.getEntvhclDt());
                continue;
            }

            if (isInoutExclude && isInternalExit(visitCar)) {
                log.info("[위반횟수 계산 - 내부입출차확인] 차량번호: {}, 내부입차게이트: {}, 내부출차게이트: {}",
                        visitCar.getCarNo(), visitCar.getEntranceGateId(), visitCar.getExitGateId());

                VisitCarDto matchedOuterVisit = findMatchedOuterVisit(visitCar, visitCarList);
                if (matchedOuterVisit != null) {
                    log.info("[위반횟수 계산 - 외부입출차매칭] 차량번호: {}, 외부입차시간: {}, 외부출차시간: {}",
                            matchedOuterVisit.getCarNo(),
                            matchedOuterVisit.getEntvhclDt(),
                            matchedOuterVisit.getLvvhclDt());
                    violationCount = processMatchedVisit(matchedOuterVisit, visitCar, isPaymentEnabled, rule, violationCount);
                } else {
                    log.info("[위반횟수 계산 - 매칭실패] 차량번호: {}, 매칭되는 외부입출차 없음", visitCar.getCarNo());
                }
            } else {
                log.info("[위반횟수 계산 - 단일입출차] 차량번호: {}, 입차시간: {}, 출차시간: {}",
                        visitCar.getCarNo(), visitCar.getEntvhclDt(), visitCar.getLvvhclDt());
                violationCount = processSingleVisit(visitCar, isPaymentEnabled, rule, violationCount);
            }
        }
        return violationCount;
    }
    private int processMatchedVisit(VisitCarDto outerVisit, VisitCarDto innerVisit,
                                    boolean isPaymentEnabled, WarningCarAutoRegistRules rule, int violationCount) {
        log.info("[매칭입출차 처리 시작] 차량번호: {}, 외부입차시간: {}, 내부입차시간: {}",
                outerVisit.getCarNo(), outerVisit.getEntvhclDt(), innerVisit.getEntvhclDt());

        Duration outerParkingDuration = Duration.between(outerVisit.getEntvhclDt(), outerVisit.getLvvhclDt());
        Duration innerParkingDuration = Duration.between(innerVisit.getEntvhclDt(), innerVisit.getLvvhclDt());
        long parkingMinutes = outerParkingDuration.minus(innerParkingDuration).toMinutes();

        log.info("[매칭입출차 - 주차시간계산] 차량번호: {}, 외부주차시간: {}분, 내부주차시간: {}분, 실제주차시간: {}분",
                outerVisit.getCarNo(),
                outerParkingDuration.toMinutes(),
                innerParkingDuration.toMinutes(),
                parkingMinutes);

        if (isPaymentEnabled) {

            int discountMinutes = parkingDiscountService.getTotalDiscountMinutes(outerVisit.getVisitCarId(), outerVisit.getEntvhclDt(), outerVisit.getCarSection());
            parkingMinutes -= discountMinutes;

            boolean wasSimpleEntryButton = outerVisit.getCarSection() >= 91 && outerVisit.getCarSection() <= 95;
            log.info("[매칭입출차 - 할인적용] 차량번호: {}, 할인시간: {}분, 최종주차시간: {}분, 간편버튼: {}",
                    outerVisit.getCarNo(), discountMinutes, parkingMinutes, wasSimpleEntryButton ? "Y" : "N");
        }

        long violationTimeMinutes = rule.getParkingTimeMinutes() + (rule.getParkingTime() * 60);
        if (parkingMinutes >= violationTimeMinutes) {
            violationCount++;
            log.info("[매칭입출차 - 위반확인] 차량번호: {}, 최종주차시간: {}분, 제한시간: {}분 => 위반(위반횟수: {})",
                    outerVisit.getCarNo(), parkingMinutes, violationTimeMinutes, violationCount);
        } else {
            log.info("[매칭입출차 - 위반확인] 차량번호: {}, 최종주차시간: {}분, 제한시간: {}분 => 정상",
                    outerVisit.getCarNo(), parkingMinutes, violationTimeMinutes);
        }

        return violationCount;
    }
    private int processSingleVisit(VisitCarDto visitCar, boolean isPaymentEnabled,
                                   WarningCarAutoRegistRules rule, int violationCount) {
        log.info("[단일입출차 처리 시작] 차량번호: {}, 입차시간: {}, 출차시간: {}",
                visitCar.getCarNo(), visitCar.getEntvhclDt(), visitCar.getLvvhclDt());

        Duration parkingDuration = Duration.between(visitCar.getEntvhclDt(), visitCar.getLvvhclDt());
        long parkingMinutes = parkingDuration.toMinutes();

        log.info("[단일입출차 - 주차시간계산] 차량번호: {}, 주차시간: {}분", visitCar.getCarNo(), parkingMinutes);

        if (isPaymentEnabled) {
            int discountMinutes = parkingDiscountService.getTotalDiscountMinutes(visitCar.getVisitCarId(), visitCar.getEntvhclDt(), visitCar.getCarSection());
            parkingMinutes -= discountMinutes;

            boolean wasSimpleEntryButton = visitCar.getCarSection() >= 91 && visitCar.getCarSection() <= 95;
            log.info("[단일입출차 - 할인적용] 차량번호: {}, 할인시간: {}분, 최종주차시간: {}분, 간편버튼: {}",
                    visitCar.getCarNo(), discountMinutes, parkingMinutes, wasSimpleEntryButton ? "Y" : "N");
        }

        long violationTimeMinutes = rule.getParkingTimeMinutes() + (rule.getParkingTime() * 60);
        if (parkingMinutes >= violationTimeMinutes) {
            violationCount++;
            log.info("[단일입출차 - 위반확인] 차량번호: {}, 최종주차시간: {}분, 제한시간: {}분 => 위반(위반횟수: {})",
                    visitCar.getCarNo(), parkingMinutes, violationTimeMinutes, violationCount);
        } else {
            log.info("[단일입출차 - 위반확인] 차량번호: {}, 최종주차시간: {}분, 제한시간: {}분 => 정상",
                    visitCar.getCarNo(), parkingMinutes, violationTimeMinutes);
        }

        return violationCount;
    }
    private boolean isInternalExit(VisitCarDto visitCar) {
        boolean isInternal = visitCar.getEntranceGateId() != 0 &&
                getGateType(visitCar.getEntranceGateId()) == 2 &&
                visitCar.getExitGateId() != null &&
                getGateType(visitCar.getExitGateId()) == 4;

        log.info("[내부입출차 여부확인] 차량번호: {}, 입차게이트: {}, 출차게이트: {}, 내부입출차여부: {}",
                visitCar.getCarNo(), visitCar.getEntranceGateId(), visitCar.getExitGateId(),
                isInternal ? "Y" : "N");

        return isInternal;
    }

    private VisitCarDto findMatchedOuterVisit(VisitCarDto internalVisit, List<VisitCarDto> visitCarList) {
        log.info("[외부입출차 매칭검색 시작] 내부입차시간: {}, 내부출차시간: {}",
                internalVisit.getEntvhclDt(), internalVisit.getLvvhclDt());

        return visitCarList.stream()
                .filter(v -> v.getEntranceGateId() != 0 &&
                        getGateType(v.getEntranceGateId()) == 1 &&
                        v.getExitGateId() != null &&
                        getGateType(v.getExitGateId()) == 3 &&
                        v.getEntvhclDt().isBefore(internalVisit.getEntvhclDt()) &&
                        v.getLvvhclDt().isAfter(internalVisit.getLvvhclDt()))
                .findFirst()
                .orElse(null);
    }

    private WarningCarAutoRegistRulesDto processViolationResult(WarningCarAutoRegistRulesDto dto,
                                                                int violationCount, WarningCarAutoRegistRules rule, String carNo) {
        log.info("$$$$$$$$$$$$$$$$$ 룰 판정시작 $$$$$$$$$$$$$$$$$$$$$$$$");
        boolean isViolation = violationCount >= rule.getViolationTime();
        dto.setActualViolationCount(violationCount);

        log.info("[최종 위반판정] 대상 등록항목: {}, 차량번호: {}, 기준위반횟수: {}, 실제위반횟수: {}, 위반여부: {}",
                dto.getCarSection(), rule.getViolationTime(), violationCount, isViolation ? "위반" : "위반아님");

        // 위반인 경우만 dto를 리턴하고, 아닌 경우는 null을 리턴하여 다음 정책 체크가 가능하도록 함
        return isViolation ? dto : null;
    }

    private Integer getGateType(Integer gateId) {
        Optional<Gate> gate = gateRepository.findById(gateId.longValue());
        return gate.isPresent() ? gate.get().getGateType() : 0;
    }

    private void checkSettingsFowardAndSend(LprRequestDto requestDto) {
        if (!needToForward) {
            return;
        }
        // 통로가 forwardGates에 있는가?
        if (forwardGates.containsKey(requestDto.getGateId())) {
            // forwardGates에 있는 통로라면, forward로 보낸다.
            sendForwardServer(requestDto, forwardGates.get(requestDto.getGateId()));
        }

    }

    private void sendForwardServer(LprRequestDto requestDto, Long toGateId) {
        log.info("차량번호 = ({}, {}), 통로 = {}({}) 를 다른 현장 {}로 보냅니다.", requestDto.getLprCarNo(), requestDto.getLprCarNo2(), requestDto.getGateId(), toGateId);
        // thread를 생성하여 url로 보낸다.
        var t = new Thread(() -> {
            interlockService.forwardGate(requestDto, forwardUrl, toGateId);
        });
        // time out은 10초로 설정
        t.start();


    }

}
