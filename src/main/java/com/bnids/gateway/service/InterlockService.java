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

import com.bnids.gateway.dto.InterlockResponseDto;
import com.bnids.gateway.dto.LocalServerRequestDto;
import com.bnids.gateway.dto.LprRequestDto;
import com.bnids.gateway.dto.SignageServerRequestDto;
import com.bnids.property.AppSetting;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public void sendGateServer(Long gateId) {
        String gateServer = appSetting.getGateControlServer();

        Mono<InterlockResponseDto> gateResponse  = webClient.get()
                .uri(gateServer+"/{gateId}", gateId)
                .retrieve()
                .bodyToMono(InterlockResponseDto.class);
        gateResponse
                .doOnError(t-> log.error("Gate Server API:{}, params:{}",gateServer,gateId,t))
                .subscribe(s-> log.info("Gate Server API:{}, params:{}, response:{}",gateServer,gateId,s));
    }

    public void sendLocalServer(LprRequestDto dto, String lprCarNo, Integer gateType, Long carSection, Integer gateStatus) {
        sendLocalServer(dto, lprCarNo, gateType, carSection, gateStatus, "", "");
    }

    public void sendLocalServer(LprRequestDto dto, String lprCarNo, Integer gateType, Long carSection, Integer gateStatus, String telNo, String visitPlaceName) {
        LocalServerRequestDto localServerRequestDto = LocalServerRequestDto.builder()
                .carNo(dto.getCarNo())
                .lprCarNo(lprCarNo)
                .gateId(dto.getGateId())
                .gateType(gateType)
                .carSection(carSection)
                .gateStatus(gateStatus)
                .carImage(dto.getCarImage())
                .telNo(telNo)
                .visitPlaceName(visitPlaceName)
                .build();

        String localServer = appSetting.getLocalServer();

        Mono<InterlockResponseDto> localResponse  = webClient.post()
                .uri(localServer)
                .syncBody(localServerRequestDto)
                //.body(BodyInserters.fromObject(localServerRequestDto))
                .retrieve()
                .bodyToMono(InterlockResponseDto.class);

        localResponse
                .doOnError(t-> log.error("Local Server API:{}, params:{}",localServer,localServerRequestDto,t))
                .subscribe(s-> log.info("Local Server API:{}, params:{}, response:{}",localResponse,localResponse,s));
    }

    public void sendSignageServer(LprRequestDto dto, Integer registItemId) {
        SignageServerRequestDto signageServerRequestDto = SignageServerRequestDto.builder()
                .carNo(dto.getCarNo())
                .gateId(dto.getGateId())
                .registItemId(registItemId)
                .build();

        String signageInterfaceServer = appSetting.getSignageInterfaceServer();

        Mono<InterlockResponseDto> signageResponse  = webClient.post()
                .uri(signageInterfaceServer)
                .syncBody(signageServerRequestDto)
                .retrieve()
                .bodyToMono(InterlockResponseDto.class);

        signageResponse
                .doOnError(t->{
                    log.error("Signage Server API:{}, params:{}",signageInterfaceServer,signageServerRequestDto,t);
                })
                .subscribe(s->{
                    log.info("Signage Server API:{}, params:{}, response:{}",signageInterfaceServer,signageServerRequestDto,s);
                });
    }
}
