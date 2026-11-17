package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.LoginIdentity;
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


    public PaymentRes.PrepareDto prepare(LoginIdentity loginIdentity , int amount , String payType) {
        String paymentId = paymentService.createPayment(loginIdentity , amount , payType);

        if(payType.equals(Define.NAVER)) {
            return PaymentRes.PrepareDto.builder()
                    .channelKey(portOneApiConst.getNaverChannelKey())
                    .paymentId(paymentId)
                    .storeId(portOneApiConst.getNaverStoreId())
                    .amount(amount)
                    .build();
        }
        else if(payType.equals(Define.KAKAO)){
            return PaymentRes.PrepareDto.builder()
                    .channelKey(portOneApiConst.getKakaoChannelKey())
                    .paymentId(paymentId)
                    .storeId(portOneApiConst.getKakaoStoreId())
                    .amount(amount)
                    .build();
        }
        return null;
    }


    public PaymentRes.CompleteDto complete(LoginIdentity loginIdentity , String paymentId , String payType) {
        PaymentRes.PortOnePayment portOnePayment;

        // 1.포트원에서 결제조회건 가져옴. (portOnePayment)
        try {
             portOnePayment = portOneApiConst.paymentInfo(paymentId , payType);
        } catch(HttpClientErrorException e) {
            throw new RuntimeException(e.getMessage());
        } catch (ResourceAccessException e){
            throw new RuntimeException(e);
        }

        // 2. 위 조회 결과로 내부에서 비교.
        return paymentService.complete(loginIdentity , portOnePayment , paymentId);
    }

}
