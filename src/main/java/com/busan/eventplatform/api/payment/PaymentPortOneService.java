package com.busan.eventplatform.api.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPortOneService {
    private final PaymentService paymentService;
    private final PortOneApiConst portOneApiConst;

    public PaymentRes.PrepareDto prepare(Long userId , int amount) {
        String paymentId = paymentService.createPayment(userId , amount);

        return PaymentRes.PrepareDto.builder()
                .channelKey(portOneApiConst.getNaverChannelKey())
                .paymentId(paymentId)
                .storeId(portOneApiConst.getNaverStoreId())
                .amount(amount)
                .build();
    }


    public PaymentRes.CompleteDto complete(Long userId , String paymentId) {
        PaymentRes.PortOnePayment portOnePayment;

        // 1.포트원에서 결제조회건 가져옴. (portOnePayment)
        try {
             portOnePayment = portOneApiConst.paymentInfo(paymentId);
        } catch(HttpClientErrorException e) {
            throw new RuntimeException(e.getMessage());
        } catch (ResourceAccessException e){
            throw new RuntimeException(e);
        }

        // 2. 위 조회 결과로 내부에서 비교.
        return paymentService.complete(userId , portOnePayment , paymentId);
    }


}
