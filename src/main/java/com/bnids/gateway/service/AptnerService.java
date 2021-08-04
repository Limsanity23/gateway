package com.bnids.gateway.service;

import com.bnids.gateway.dto.AptnerReserve;
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

    /*private  String APTNER_SERVER = "https://devgtw.aptner.com/pc";
    private  String KAPT_CODE = "T77777777";
    private  String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJBUFRORVIiLCJhdWQiOiJCTiIsImV4cCI6MzE4MDc0NTEyNiwiaWF0IjoxNjI1NTQ1MTI2LCJyb2xlcyI6IlBDIn0.804VuU5GEw86QEdf23sTaKIYF0vm8s1-SrcGuD931Pc";*/

    private  String APTNER_SERVER = "https://gtw.aptner.com/pc";
    private  String KAPT_CODE = "A41576914";
    private  String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJBUFRORVIiLCJhdWQiOiJCTiIsImV4cCI6MzE4MDc0NTEyNiwiaWF0IjoxNjI1NTQ1MTI2LCJyb2xlcyI6IlBDIn0.804VuU5GEw86QEdf23sTaKIYF0vm8s1-SrcGuD931Pc";


    /**
     * 방문예약 전체목록 가져오기
     * @return
     * @throws ParseException
     */
    public List<AptnerReserve> getAptnerVisitAll() throws ParseException {
        log.info("* getAptnerVisitAll *");
        SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd");
        Date time = new Date();
        String today = format1.format(time);

        AptnerVisitAllResponseDto aptResponse = webClient.get()
                .uri(APTNER_SERVER + "/visit/all?kaptCode="+KAPT_CODE+"&searchDate="+today)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
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

    /**
     * 입차통보
     * @param carNo
     * @param dong
     * @param ho
     */
    public void sendAccessIn(String carNo, String dong, String ho) {
        log.info("* sendAccessIn *");
        log.info("* 차량번호: {}, 동: {}, 호:{} 아파트너 입차통보 *", carNo, dong, ho);

//        AptnerAccessInRequestDto dto = AptnerAccessInRequestDto.builder()
//                .carNo(carNo)
//                .dong(dong)
//                .ho(ho)
//                .isResident("N")
//                .build();

        Mono<String > response = webClient.post()
                .uri(APTNER_SERVER+"/access/in?kaptCode="+KAPT_CODE+"&carNo="+carNo+"&dong="+dong+"&ho="+ho+"&isResident=N")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
//                .syncBody(dto)
                .retrieve()
                .bodyToMono(String.class);

        response
                .doOnError(t -> {
                    log.error("아파트너 입차 통보 : 차량번호 = {}, 실패 응답 = {}", carNo, t);
                })
                .subscribe(s -> {
                    log.info("아파트너 입차 통보 : 차량번호 = {}, 성공 응답 = {}", carNo, s);
                });
    }

}


