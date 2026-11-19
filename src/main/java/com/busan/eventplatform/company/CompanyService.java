package com.busan.eventplatform.company;

import com.busan.eventplatform.company.businessverification.BusinessVerificationSessionService;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.global.file.FileStorage;
import com.busan.eventplatform.global.file.FileStorageResult;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final BusinessVerificationSessionService businessVerificationSessionService;
    private final FileStorage fileStorage;
    private final PlatformTransactionManager platformTransactionManager;

    //    DB 수정 작업을 어디서 시작하고 끝낼지 정하는 설정
    private TransactionTemplate createWriteTransaction() {
//        spring의 실제 DB 트랜잭션 관리자인 transactionManager를 이용해 실행 도구를 만든다
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
//        실행할 때 별도의 새 트랜잭션을 사용한다.
        transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        return transactionTemplate;
    }

    public CompanyResponse.DetailDTO findByDetail(Long id) {
        Company company = companyRepository.findById(id).orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));
        return new CompanyResponse.DetailDTO(company);
    }

    public CompanyResponse.UpdateDTO findByUpdate(Long id) {
        Company company = companyRepository.findById(id).orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));
        return new CompanyResponse.UpdateDTO(company);
    }


    //    기업 회원 정보 수정
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CompanyResponse.DetailDTO update(Long id, CompanyRequest.UpdateDTO updateDTO, HttpSession session) {

        TransactionTemplate transactionTemplate = createWriteTransaction();

        final FileStorageResult storedFile = updateDTO.hasBusinessCertificate() ? fileStorage.upload(updateDTO.getBusinessCertificate()) : null;

        try {
            return transactionTemplate.execute(transactionStatus -> {
                Company company = companyRepository.LockFindById(id).orElseThrow(() -> new Exception404("해당 기업을 찾을 수 없습니다."));

//        새 사업자 등록증을 첨부했는지
                boolean certificateUpload = updateDTO.hasBusinessCertificate();
//        기업인증정보가 변경되었는지
                boolean companyInformationChanged = updateDTO.requiresReview(company);
//        사업자번호 변경 판단
                boolean businessNumberChanged = updateDTO.getBusinessNumber() != null && !company.getBusinessNumber().equals(updateDTO.getBusinessNumber());
//        대표자 변경 판단
                boolean representativeNameChanged = updateDTO.getRepresentativeName() != null && !company.getRepresentativeName().equals(updateDTO.getRepresentativeName());
//        개업일 변경 판단
                boolean openedOnChanged = updateDTO.getOpenedOn() != null && !company.getOpenedOn().equals(updateDTO.getOpenedOn());

                // TODO: 기존 DB에 API 검증 상태가 ERROR인 기업이 남아 있다면,
                //  사업자 정보 변경 없이도 재검증 결과를 DB에 반영할 수 있는 흐름 검토.
                //  현재 신규 가입은 MATCHED만 허용하므로 기존 데이터 지원 여부에 따라 진행.
                //  PENDING 상태의 재검증 허용 여부도 함께 결정 필요.
//        사업자 인증이 필요한지 판단
                boolean requiresBusinessVerification = businessNumberChanged || representativeNameChanged || openedOnChanged;
//        정보변경 또는 사업자등록증 업로드시 관리자 재심사 필요
                boolean requiresReview = companyInformationChanged || certificateUpload;

//        PENDING 상태에서는 기업 인증 정보 변경과 등록증 교체 불가
                if (company.getStatus() == CompanyStatus.PENDING && requiresReview) {
                    throw new Exception400("검토 중에는 기업 인증 정보 수정이 불가합니다.");
                }

//        기업인증을 변경하고 사업자 등록증 첨부가 안되면 수정 불가.
                if (companyInformationChanged && !certificateUpload) {
                    throw new Exception400("기업 인증 정보를 변경하려면 새 사업자 등록증을 첨부해주세요");
                }

                if (businessNumberChanged) { // 사업자 번호가 변경되면, 중복확인
                    companyRepository.findByBusinessNumber(updateDTO.getBusinessNumber())
                            .ifPresent(c -> {
                                if (!Objects.equals(c.getId(), company.getId())) {
                                    throw new Exception400("이미 등록되어있는 사업자번호입니다.");
                                }
                            });
                }

//        현재 사업자 인증 상태를 불러옴
                BusinessVerificationStatus verificationStatus = company.getBusinessVerificationStatus();

//        사업자 정보가 변경되면 해당 정보의 검증 성공 기록 확인
                if (requiresBusinessVerification) {
                    String businessNumber = updateDTO.getBusinessNumber() != null ? updateDTO.getBusinessNumber() : company.getBusinessNumber();
                    String representativeName = updateDTO.getRepresentativeName() != null ? updateDTO.getRepresentativeName() : company.getRepresentativeName();
                    LocalDate openedOn = updateDTO.getOpenedOn() != null ? updateDTO.getOpenedOn() : company.getOpenedOn();

                    businessVerificationSessionService.requireVerifiedBusiness(businessNumber, representativeName, openedOn, session);
                    verificationStatus = BusinessVerificationStatus.MATCHED;
                    company.recordBusinessVerification(verificationStatus);

//                     DB 커밋 성공 후 사업자 검증 세션을 정리하도록 등록
                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    try {
                                        businessVerificationSessionService.clear(session);
                                    } catch (RuntimeException cleanUpFailure) {
                                        log.warn(
                                                "기업 수정 성공 후 사업자 검증 세션 정리 실패",
                                                cleanUpFailure
                                        );
                                    }
                                }
                            }
                    );
                }

//        검사 후 결과에 따라 최종 Status가 결정된다.
                if (requiresReview) {
                    if (verificationStatus == BusinessVerificationStatus.MISMATCHED) {
                        company.requestCorrection(); // FEEDBACK_NEEDED
                    } else {
                        company.requestReview(); // PENDING
                    }
                }

//                새 파일키로 변경하기 전, 기존 키 기억
                String previousStorageKey = company.getBusinessCertificateFileName();

//                새 파일로 교체되는 경우, DB 커밋 성공 후 기존 파일 삭제
                if (storedFile != null
                        && previousStorageKey != null
                        && !previousStorageKey.isBlank()
                        && !previousStorageKey.equals(storedFile.getStorageKey())) {
                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    try {
                                        fileStorage.delete(previousStorageKey);
                                    } catch (RuntimeException cleanUpFailure) {
                                        log.error(
                                                "기업 수정 완료 후 기존 등록증 파일 삭제 실패: {}",
                                                previousStorageKey,
                                                cleanUpFailure
                                        );
                                    }
                                }
                            }
                    );
                }

//                위 조건을 모두 통과하면 DB 데이터 수정이 이루어짐
                company.update(
                        updateDTO.getName(),
                        updateDTO.getRepresentativeName(),
                        updateDTO.getBusinessNumber(),
                        updateDTO.getAddress(),
                        updateDTO.getContactPersonName(),
                        updateDTO.getContactPersonPhone(),
                        updateDTO.getOpenedOn(),
                        storedFile != null ? storedFile.getStorageKey() : null
                );

//                비밀번호 해싱
                if (updateDTO.getPassword() != null) {
                    String encodedPassword = passwordEncoder.encode(updateDTO.getPassword());
                    company.changePassword(encodedPassword);
                }

                return new CompanyResponse.DetailDTO(company);
            });

        } catch (RuntimeException | Error failure) {
            if (storedFile != null) {
                try {
                    fileStorage.delete(storedFile.getStorageKey());
                } catch (RuntimeException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);

                    log.error("기업 수정 실패 후 새 등록증 파일 정리 실패", cleanupFailure);
                }
            }

            throw failure;
        }


    }

//    TODO 기업 회원 탈퇴 (임시보류)

}
