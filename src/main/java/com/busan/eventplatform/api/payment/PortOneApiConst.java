package com.busan.eventplatform.api.payment;

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
    @Value("${portone.store-id}")
    private String naverStoreId;

    @Value("${portone.channel-key}")
    private String naverChannelKey;

    @Value("${portone.secret-key}")
    private String naverSecretKey;

    private final String BASE_URL = "https://api.portone.io/payments/";
    private final RestTemplate restTemplate = new RestTemplate();

    public PaymentRes.PortOnePayment paymentInfo(String paymentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION , "PortOne " + naverSecretKey);

        HttpEntity<?> req = new HttpEntity<>(headers);

        ResponseEntity<PaymentRes.PortOnePayment> res = restTemplate.exchange(BASE_URL + paymentId , HttpMethod.GET
                , req , PaymentRes.PortOnePayment.class);

        return res.getBody();
    }
}
