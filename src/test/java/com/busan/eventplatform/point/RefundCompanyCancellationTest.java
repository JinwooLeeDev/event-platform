package com.busan.eventplatform.point;

import com.busan.eventplatform.api.purchase.*;
import com.busan.eventplatform.api.refund.*;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventCancel.*;
import com.busan.eventplatform.event.eventSession.*;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.registration.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundCompanyCancellationTest {
    RefundRepository refunds;
    RegistrationRepository registrations;
    EventSessionRepository sessions;
    RefundService service;
    Company company;
    EventSession session;
    Deposit deposit;
    LocalDate requestedOn=LocalDate.of(2030,1,7);
    @BeforeEach void setup(){
        refunds=mock(RefundRepository.class);registrations=mock(RegistrationRepository.class);sessions=mock(EventSessionRepository.class);
        var deposits=mock(DepositRepository.class);
        service=new RefundService(mock(PurchaseRepository.class),refunds,sessions,deposits,registrations,mock(UserRepository.class));
        company=Company.builder().build();
        ReflectionTestUtils.setField(company,"id",1L);ReflectionTestUtils.setField(company,"point",0);
        session=EventSession.builder().id(2L).event(Event.builder().company(company).build())
                .status(EventSessionStatus.CANCELLED).startsAt(LocalDate.of(2030,1,10).atTime(12,0)).build();
        deposit=Deposit.builder().company(company).eventSession(session).build();deposit.setDepositPaid();
        ReflectionTestUtils.setField(deposit,"id",3L);
        when(sessions.findByCompanyIdAndSessionId(1L,2L)).thenReturn(Optional.of(session));
        when(deposits.findByCompanyIdAndSessionId(1L,2L)).thenReturn(Optional.of(deposit));
        when(refunds.findByDepositId(3L,RefundStatus.PENDING)).thenReturn(Optional.empty());
        when(refunds.save(any(Refund.class))).thenAnswer(call->call.getArgument(0));
    }
    @Test void missingPendingRefundIsCreatedAndCompletedUsingRequestDate(){
        service.refundCompany(company,2L,requestedOn,"취소 사유");
        var captured=ArgumentCaptor.forClass(Refund.class);verify(refunds).save(captured.capture());
        var refund=captured.getValue();
        assertThat(refund.getDeposit()).isSameAs(deposit);assertThat(refund.getCompany()).isSameAs(company);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND);assertThat(refund.getAmount()).isEqualTo(100000);
        assertThat(refund.getReason()).isEqualTo("취소 사유");assertThat(company.getPoint()).isEqualTo(100000);
        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.REFUNDED);
    }
    @Test void existingPendingRefundIsReused(){
        var pending=Refund.companyBuilder().company(company).deposit(deposit).reason("이전 사유").build();
        when(refunds.findByDepositId(3L,RefundStatus.PENDING)).thenReturn(Optional.of(pending));
        service.refundCompany(company,2L,requestedOn,"취소 사유");
        verify(refunds,never()).save(any());assertThat(pending.getStatus()).isEqualTo(RefundStatus.REFUND);
        assertThat(pending.getAmount()).isEqualTo(100000);assertThat(company.getPoint()).isEqualTo(100000);
    }
    @Test void secondRefundIsRejectedWithoutAnotherCreditOrRecord(){
        service.refundCompany(company,2L,requestedOn,"취소 사유");
        assertThatThrownBy(()->service.refundCompany(company,2L,requestedOn,"취소 사유")).isInstanceOf(Exception400.class);
        verify(refunds,times(1)).save(any());assertThat(company.getPoint()).isEqualTo(100000);
    }
    @Test void unpaidDepositStillCannotBeRefunded(){
        ReflectionTestUtils.setField(deposit,"status",DepositStatus.READY);
        assertThatThrownBy(()->service.refundCompany(company,2L,requestedOn,"취소 사유")).isInstanceOf(Exception400.class);
        verify(refunds,never()).save(any());assertThat(company.getPoint()).isZero();
    }
    @Test void administratorApprovalCompletesWithoutPrecreatedRefund(){
        session.setStatus(EventSessionStatus.SCHEDULED);
        var cancellations=mock(EventCancelRepository.class);var registrationService=mock(RegistrationService.class);
        var request=EventCancel.builder().id(4L).eventSession(session).status(EventCancelStatus.PENDING)
                .reason("취소 사유").createdAt(requestedOn.atStartOfDay()).build();
        when(cancellations.findById(4L)).thenReturn(Optional.of(request));
        var participant=User.builder().point(1000).build();
        var purchase=Purchase.builder().user(participant).eventSession(session).price(6000).build();
        var registration=Registration.create(participant,session,purchase,RegistrationStatus.CONFIRMED,1);
        when(registrations.findAllBySessionIdWithUser(2L)).thenReturn(List.of(registration));
        doAnswer(call->{assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.REFUNDED);return null;})
                .when(registrationService).cancelAllBySession(2L,"취소 사유");
        new EventCancelService(cancellations,sessions,service,registrationService).approve(4L);
        assertThat(request.getStatus()).isEqualTo(EventCancelStatus.APPROVED);
        assertThat(session.getStatus()).isEqualTo(EventSessionStatus.CANCELLED);
        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.REFUNDED);assertThat(company.getPoint()).isEqualTo(100000);
        assertThat(participant.getPoint()).isEqualTo(7000);
        var captured=ArgumentCaptor.forClass(Refund.class);verify(refunds,times(2)).save(captured.capture());
        assertThat(captured.getAllValues()).anySatisfy(refund->{
            assertThat(refund.getUser()).isSameAs(participant);assertThat(refund.getAmount()).isEqualTo(6000);
        });
        verify(registrationService).cancelAllBySession(2L,"취소 사유");
    }
}
