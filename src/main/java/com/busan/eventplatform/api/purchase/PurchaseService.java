package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.AlreadyPurchaseException;
import com.busan.eventplatform.global.error.exception.Exception404;
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
    private final RegistrationRepository registrationRepository;
    private final EventSessionRepository eventSessionRepository;

    // 결제창에 필요한 행사 결제 포인트와 행사 정보 조회.
    public PurchaseRes.PurchasePointDto calcPointAndEvent(Long userId, Long eventSessionId) {
        EventSession eventSession = eventSessionRepository.findByIdWithEvent(eventSessionId)
                .orElseThrow(() -> new Exception404("결제하려는 행사가 존재하지 않습니다"));

            User user = userRepository.findById(userId).orElseThrow(() -> new Exception404("유저를 찾을 수 없습니다"));

            return PurchaseRes.PurchasePointDto.builder()
                    .eventSession(eventSession)
                    .currentPoint(user.getPoint())
                    .build();
    }


    // 행사 결제 기능.
    @Transactional
    public Purchase purchase(Long eventSessionId, Long userId) {
        EventSession eventSession = eventSessionRepository.findById(eventSessionId)
                        .orElseThrow(() -> new Exception404("결제하려는 행사가 존재하지 않습니다"));

            User user = userRepository.findById(userId)
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

            return purchase;
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
