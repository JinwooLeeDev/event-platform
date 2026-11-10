package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.error.exception.AlreadyPurchaseException;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.registration.Registration;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RegistrationRepository registrationRepository;

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


    public List<PurchaseRes.PurchaseListDto> purchaseList(Long userId) {
        List<Purchase> purchaseList = purchaseRepository.findByUserIdWithPurchaseList(userId);
        if(purchaseList == null) return List.of(); // 구매내역 없으면 그냥 빈 목록으로.

        // registrationList 조회할때 동일 userId로 조회하므로 이벤트세션ID만 필요. --> map으로
        List<Long> sessionIds =purchaseList.stream()
                .map(p -> p.getEventSession().getId())
                .distinct() // 동일 회차 등록 제거.
                .toList();

        List<Registration> registrationList =
                registrationRepository.findAllByUserIdAndSessionIds(userId , sessionIds);

        Map<Long, Registration> registrationMap = new HashMap<>();

        for(Registration r : registrationList) {
            Long eventSessionId = r.getEventSession().getId();
            Registration savedRegistration = registrationMap.get(eventSessionId);

            if(savedRegistration == null || r.getId() > savedRegistration.getId()) {
                registrationMap.put(r.getEventSession().getId() , r);
            }
        }

        return purchaseList.stream()
                .map(p ->
                        new PurchaseRes.PurchaseListDto(p , registrationMap.get(p.getEventSession().getId())))
                .toList();
    }
}
