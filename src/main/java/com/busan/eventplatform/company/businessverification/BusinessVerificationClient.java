package com.busan.eventplatform.company.businessverification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class BusinessVerificationClient {
    private final RestClient restClient; // 실제 HTTP 요청을 보냄
    private final String baseUrl;
    private final String serviceKey;

    public BusinessVerificationClient(
            @Value("${app.business-verification.base-url}") String baseUrl, // yaml 설정값을 생성자로 받음.
            @Value("${app.business-verification.service-key}") String serviceKey
    ) {
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;

//        HTTP 연결을 처리하는 객체
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000); // 국세청 서버 연결시간 3초
        factory.setReadTimeout(5000); // 연결 후 응답을 읽는 대기 제한 5초

//        HTTP 요청 도구 설정
        this.restClient = RestClient.builder()
                .requestFactory(factory) // 연결 읽기 제한 시간 사용.
                .build();
    }

//    외부 호출 메서드
    public BusinessVerificationResponse.Validate validate(BusinessVerificationRequest.Validate request) {

//        기본 주소를 바탕으로 요청 주소를 만듦 - https://api.odcloud.kr/api/nts-businessman/v1 <- 이 주소 뒤에 /validate?serviceKey={serviceKey(인코딩)}
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/validate")
                .queryParam("serviceKey", "{serviceKey}")
                .encode()
                .buildAndExpand(serviceKey)
                .toUri();

        // uri 생성 완료 후 post 방식 요청준비. 국세청 API는 POST로만 행함.
        return restClient.post()
                .uri(uri) //주소 입력
                .contentType(MediaType.APPLICATION_JSON) // 보내는 본문 JSON (공식문서 참고)
                .accept(MediaType.APPLICATION_JSON) // 응답도 JSON으로 요청
                .body(request) //전달받은 요청 DTO를 HTTP 본문에 넣는다. Spring이 JSON으로 변환
                .retrieve() // 응답처리. 4xx, 5xx는 예외처리
                .body(BusinessVerificationResponse.Validate.class); // 이 호출이 이어져야 실제 요청을 수행하고 응답 본문을 받음. 받을 데이터의 타입이다.
    }




}
