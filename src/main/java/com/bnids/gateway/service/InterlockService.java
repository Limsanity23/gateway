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
