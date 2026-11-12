package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.error.exception.AlreadyPurchaseException;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RegistrationService registrationService;

    // 결제창에 필요한 행사 결제 포인트와 행사 정보 조회.
    // TODO 추후 eventSession 생성되면 eventRepository에서 쿼리 가져와서 eventSession 조회.
    public PurchaseRes.PurchasePointDto calcPoint(LoginIdentity loginIdentity, Long eventId) {
        EventSession eventSession = purchaseRepository.findByIdWithEvent(eventId)
                .orElseThrow(() -> new Exception404("결제하려는 행사가 존재하지 않습니다"));

        /// User 분기점
        if(loginIdentity.isUser()) {
            User user = userRepository.findById(loginIdentity.getId())
                    .orElseThrow(() -> new Exception404("유저를 찾을 수 없습니다"));

            return PurchaseRes.PurchasePointDto.builder()
                    .id(eventSession.getId())
                    .currentPoint(user.getPoint())
                    .eventPoint(eventSession.getFee())
                    .build();
        }


        /// Company 분기점
        else if(loginIdentity.isCompany()) {
                Company company = companyRepository.findById(loginIdentity.getId())
                        .orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다"));

                return PurchaseRes.PurchasePointDto.builder()
                        .id(eventSession.getId())
                        .currentPoint(company.getPoint())
                        .eventPoint(eventSession.getFee())
                        .build();
        }
        else throw new Exception400("행사 결제를 진행할 수 없습니다");
    }


    // 행사 결제 기능.
    @Transactional
    public void purchase(Long eventId, LoginIdentity loginIdentity) {
        // 1. 실제 있는 이벤트인지. TODO 나중에 eventSession 구현되면 삭제.
        EventSession eventSession = purchaseRepository.findByIdWithEvent(eventId)
                        .orElseThrow(() -> new Exception404("결제하려는 행사가 존재하지 않습니다"));

        /// USER 분기점
        if(loginIdentity.isUser()) {
            User user = userRepository.findById(loginIdentity.getId())
                    .orElseThrow(() -> new Exception404("유저를 찾을 수 없습니다"));

            // 2. 유저가 이미 결제한 행사인지.
            if(purchaseRepository.existsEventByUser(user.getId() , eventSession.getId())) {
                throw new AlreadyPurchaseException("이미 결제처리된 행사입니다");
            }

            // 3. 통과했다면 포인트 차감 + 결제내역 건 생성.  포인트 부족하다면 deductPoint 내부에서 필터링.
            user.deductPoint(eventSession.getFee());

            // 4. 결제 전 대기열 등록 또는 대기열 등록상태 확인과 상태 갱신.
            registrationService.applyForEvent(user , eventSession);

            Purchase purchase = Purchase.builder()
                    .user(user)
                    .eventSession(eventSession)
                    .price(eventSession.getFee())
                    .build();

            purchaseRepository.save(purchase);
        }


        /// Company 분기점
        if(loginIdentity.isCompany()) {
            Company company = companyRepository.findById(loginIdentity.getId())
                    .orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다"));

            // 2. 기업이 이미 결제한 행사인지.
            if(purchaseRepository.existsEventByCompany(company.getId() , eventSession.getId())) {
                throw new AlreadyPurchaseException("이미 결제처리된 행사입니다");
            }

            // 4. 통과했다면 포인트 차감 + 결제내역 건 생성.
            company.deductPoint(eventSession.getFee());

            Purchase purchase = Purchase.builder()
                    .company(company)
                    .eventSession(eventSession)
                    .price(eventSession.getFee())
                    .build();

            purchaseRepository.save(purchase);
        }
    }


    // TODO 아직 구현중.
//    public List<PurchaseRes.PurchaseListDto> purchaseList(Long userId) {
//        List<PurchaseRes.PurchaseListDto> purchaseList = purchaseRepository.findByUserIdWithPurchaseList(userId);
//        return null;
//    }
}
