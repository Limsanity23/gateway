package com.bnids.gateway.service;

import com.bnids.gateway.dto.AptnerResult;
import com.bnids.gateway.dto.AptnerVisitAllResponseDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AptnerService {

    @NonNull
    private final WebClient webClient;

    public List<AptnerResult> getAptnerVisitAll() throws ParseException {
        log.info("* getAptnerVisitAll *");
        //test
        /*String aptnerServer = "https://devgtw.aptner.com/pc";
        String kaptCode = "T77777777";
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJBUFRORVIiLCJhdWQiOiJCTiIsImV4cCI6MzE4MDc0NTEyNiwiaWF0IjoxNjI1NTQ1MTI2LCJyb2xlcyI6IlBDIn0.804VuU5GEw86QEdf23sTaKIYF0vm8s1-SrcGuD931Pc";*/

        //real
        String aptnerServer = "https://gtw.aptner.com/pc"; //real
        String kaptCode = "A41576914"; //real
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJBUFRORVIiLCJhdWQiOiJCTiIsImV4cCI6MzE4MDc0NTEyNywiaWF0IjoxNjI1NTQ1MTI3LCJyb2xlcyI6IlBDIn0.3dJk09kDC_6Vc-yRBJ6kJobplwVCqnFk8a9sfGzewdA";

        SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd");
        Date time = new Date();
        String today = format1.format(time);

        AptnerVisitAllResponseDto aptResponse = webClient.get()
                .uri(aptnerServer + "/visit/all?kaptCode="+kaptCode+"&searchDate="+today)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(AptnerVisitAllResponseDto.class)
                .block();

        Mono.just(aptResponse)
                .doOnError(t -> {
                    log.error("아파트너 방문 예약 전체 목록 API 실패 응답 = {}", t);
                })
                .subscribe(s -> {
                    log.info("아파트너 방문 예약 전체 목록 API 성공 응답 = {}", s.toString());
                });

        return aptResponse.getResult();
    }

}


