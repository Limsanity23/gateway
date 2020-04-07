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

import com.bnids.gateway.dto.*;
import com.bnids.config.AppSetting;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author yannishin
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterlockService {
    @NonNull
    private final AppSetting appSetting;

    @NonNull
    private final WebClient webClient;

    /**
     *  차단기 연동 서버에 차량정보 전송
     *  인터페이스 ID PCLPR0004
     *
     * @param dto 연동요청 Dto
     */
    public void sendGateServer(InterlockRequestDto dto) {
        if (StringUtils.contains(dto.getInstallDevice(), "BLDC_GATE")) {
            String gateServer = appSetting.getGateControlServer();
            Long gateId = dto.getGateId();

            Mono<GateServerResponseDto> gateResponse = webClient.get()
                    .uri(gateServer + "/{gateId}", gateId)
                    .retrieve()
                    .bodyToMono(GateServerResponseDto.class);

            gateResponse
                    .doOnError(t -> {
                        log.error("차단기 연동 서버 = {}, 차량번호 = {}, 통로 = {}({}), 실패 응답 = {}", gateServer, dto.getCarNo(), dto.getGateName(), gateId, t);
                    })
                    .subscribe(s -> {
                        log.info("차단기 연동 서버 = {}, 차량번호 = {}, 통로 = {}({}), 성공 응답 = {}", gateServer, dto.getCarNo(), dto.getGateName(), gateId, s.getData());
                    });
        }
    }

    /**
     *  전광판 콘트롤러 연동 서버에 차량정보 전송
     *  인터페이스 ID PCLPR0002
     *
     * @param dto 연동요청 Dto
     */
    public void sendSignageServer(InterlockRequestDto dto) {
        if (StringUtils.contains(dto.getInstallDevice(), "SIGNAGE")) {
            SignageServerRequestDto signageServerRequestDto = SignageServerRequestDto.builder()
                    .carNo(dto.getCarNo())
                    .gateId(dto.getGateId())
                    .registItemId(dto.getCarSection())
                    .build();

            String signageInterfaceServer = appSetting.getSignageInterfaceServer();

            Mono<InterlockResponseDto> signageResponse = webClient.post()
                    .uri(signageInterfaceServer)
                    .syncBody(signageServerRequestDto)
                    .retrieve()
                    .bodyToMono(InterlockResponseDto.class);

            signageResponse
                    .doOnError(t -> {
                        log.error("전광판 연동 서버 = {}, 차량번호 = {}, 통로 = {}({}), 실패 응답 = {}", signageInterfaceServer, dto.getCarNo(), dto.getGateName(), dto.getGateId(), t);
                    })
                    .subscribe(s -> {
                        log.info("전광판 연동 서버 = {}, 차량번호 = {}, 통로 = {}({}), 성공 응답 = {}", signageInterfaceServer, dto.getCarNo(), dto.getGateName(), dto.getGateId(), s.getMessage());
                    });
        }
    }

    /**
     *  로컬 서버에 차량정보 전송
     *  인터페이스 ID PCLPR0003
     *
     * @param dto 연동요청 Dto
     */
    public void sendLocalServer(InterlockRequestDto dto) {
        LocalServerRequestDto localServerRequestDto = LocalServerRequestDto.builder()
                .carNo(dto.getCarNo())
                .lprCarNo(dto.getLprCarNo())
                .gateId(dto.getGateId())
                .gateName(dto.getGateName())
                .gateType(dto.getGateType())
                .carSection(dto.getCarSection())
                .gateStatus(dto.getGateStatus())
                .carImage(dto.getCarImage())
                .telNo(dto.getTelNo())
                .visitPlaceName(dto.getVisitName())
                .addressDong(dto.getAddressDong())
                .addressHo(dto.getAddressHo())
                .siteCode(dto.getSiteCode())
                .installOption(dto.getInstallOption())
                .restrictLeaveCar(0)
                .build();

        String localServer = appSetting.getLocalServer();

        Mono<InterlockResponseDto> localResponse  = webClient.post()
                .uri(localServer)
                .syncBody(localServerRequestDto)
                //.body(BodyInserters.fromObject(localServerRequestDto))
                .retrieve()
                .bodyToMono(InterlockResponseDto.class);

        localResponse
                .doOnError(t->{
                    log.error("로컬 서버 = {}, 차량번호 = {}, 통로 = {}({}), 실패 응답 = {}", localServer, dto.getCarNo(), dto.getGateName(), dto.getGateId(), t);
                })
                .subscribe(s-> {
                    log.info("로컬 서버 = {}, 차량번호 = {}, 통로 = {}({}), 성공 응답 = {}", localServer, dto.getCarNo(), dto.getGateName(), dto.getGateId(), s.getMessage());
                });
    }

    /**
     *  로컬 서버에 차량정보 전송
     *  인터페이스 ID PCLPR0003
     *
     * @param dto 연동요청 Dto
     */
    public void sendHomenetServer(InterlockRequestDto dto) {

        log.info("홈넷 전송 필요 여부 체크. InstallDevice = {}, 차량번호 = {}, 통로 = {}({}), CarSection = {}, NoticeSetup = {}, ",
                dto.getInstallDevice(), dto.getCarNo(), dto.getGateName(), dto.getGateId(), dto.getCarSection() , dto.getNoticeSetup());

        if (StringUtils.contains(dto.getInstallDevice(), "HOMENET")
                && dto.getCarSection() == 10 && StringUtils.contains(dto.getNoticeSetup(),"HOMENET")) {
            log.info("홈넷 전송 시작");
            HomenetServerRequestDto homenetServerRequestDto = HomenetServerRequestDto.builder()
                    .carNo(dto.getCarNo())
                    .gateType(dto.getGateType())
                    .gateName(dto.getGateName())
                    .addressDong(dto.getAddressDong())
                    .addressHo(dto.getAddressHo())
                    .ownerName(dto.getOwnerName())
                    .build();

            String homenetServer = appSetting.getHomenetInterfaceServer();

            Mono<InterlockResponseDto> homenetResponse = webClient.post()
                    .uri(homenetServer)
                    .syncBody(homenetServerRequestDto)
                    //.body(BodyInserters.fromObject(localServerRequestDto))
                    .retrieve()
                    .bodyToMono(InterlockResponseDto.class);
            log.info("홈넷 전송 전송 완료 응답 대기");

            homenetResponse
                    .doOnError(t -> {
                        log.error("홈넷 연동 서버 = {}, 차량번호 = {}, 통로 = {}({}), 실패 응답 = {}", homenetServer, dto.getCarNo(), dto.getGateName(), dto.getGateId(), t);
                    })
                    .subscribe(s -> {
                        log.info("홈넷 연동 서버 = {}, 차량번호 = {}, 통로 = {}({}), 성공 응답 = {}", homenetServer, dto.getCarNo(), dto.getGateName(), dto.getGateId(), s.getMessage());
                    });
        }
    }
}
