package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.global.util.Define;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Getter
@Component
public class PortOneApiConst {
    /// =====================Naver=====================
    @Value("${portone.naver-store-id}")
    private String naverStoreId;

    @Value("${portone.naver-channel-key}")
    private String naverChannelKey;

    @Value("${portone.naver-secret-key}")
    private String naverSecretKey;

    /// =====================Kakao=====================
    @Value("${portone.kakao-store-id}")
    private String kakaoStoreId;

    @Value("${portone.kakao-channel-key}")
    private String kakaoChannelKey;

    @Value("${portone.kakao-secret-key}")
    private String kakaoSecretKey;


    private final String BASE_URL = "https://api.portone.io/payments/";
    private final RestTemplate restTemplate = new RestTemplate();

    public PaymentRes.PortOnePayment paymentInfo(String paymentId , String payType) {
        HttpHeaders headers = new HttpHeaders();

        if(payType.equals(Define.NAVER))  headers.set(HttpHeaders.AUTHORIZATION , "PortOne " + naverSecretKey);
        else if(payType.equals(Define.KAKAO))  headers.set(HttpHeaders.AUTHORIZATION , "PortOne " + kakaoSecretKey);

        HttpEntity<?> req = new HttpEntity<>(headers);

        ResponseEntity<PaymentRes.PortOnePayment> res = restTemplate.exchange(BASE_URL + paymentId , HttpMethod.GET
                , req , PaymentRes.PortOnePayment.class);

        return res.getBody();
    }

}
