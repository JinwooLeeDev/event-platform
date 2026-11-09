package com.busan.eventplatform.login.companylogin;

import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.global.file.FileStorage;
import com.busan.eventplatform.global.file.FileStorageResult;
import com.busan.eventplatform.login.emailverification.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.Instant;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CompanySignupFileCleanupTest {
    @Test
    void commitFailureDeletesUploadedFileAndPreservesOriginalError() {
        CompanyRepository repository = mock(CompanyRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        EmailVerificationService verification = mock(EmailVerificationService.class);
        FileStorage storage = mock(FileStorage.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        CompanyAuthService service = new CompanyAuthService(repository, encoder, verification, storage, manager);
        CompanySignupReqDto request = request();
        MockHttpSession session = new MockHttpSession();
        when(verification.requireVerifiedEmail(request.getEmail(), session)).thenReturn(Instant.now());
        when(encoder.encode(anyString())).thenReturn("hash");
        when(storage.upload(request.getBusinessCertificate())).thenReturn(
                FileStorageResult.builder().storageKey("files/test.pdf").build());
        when(manager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        RuntimeException failure = new IllegalStateException("simulated commit failure");
        doThrow(failure).when(manager).commit(any());
        assertThatThrownBy(() -> service.signup(request, session)).isSameAs(failure);
        verify(repository).saveAndFlush(any());
        verify(storage).delete("files/test.pdf");
    }

    @Test
    void invalidFileDoesNotPersistCompany() {
        CompanyRepository repository = mock(CompanyRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        EmailVerificationService verification = mock(EmailVerificationService.class);
        FileStorage storage = mock(FileStorage.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        CompanyAuthService service = new CompanyAuthService(repository, encoder, verification, storage, manager);
        CompanySignupReqDto request = request();
        RuntimeException failure = new IllegalArgumentException("invalid file");
        when(storage.upload(request.getBusinessCertificate())).thenThrow(failure);
        assertThatThrownBy(() -> service.signup(request, new MockHttpSession())).isSameAs(failure);
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(manager);
    }

    private CompanySignupReqDto request() {
        CompanySignupReqDto request = new CompanySignupReqDto();
        request.setName("Company");
        request.setEmail("company@example.test");
        request.setPassword("Password1!");
        request.setPasswordConfirm("Password1!");
        request.setBusinessNumber("1234567890");
        request.setRepresentativeName("Representative");
        request.setOpenedOn(LocalDate.of(2020, 1, 1));
        request.setAddress("Busan");
        request.setContactPersonName("Contact");
        request.setContactPersonPhone("010-0000-0001");
        request.setBusinessCertificate(new MockMultipartFile("businessCertificate", "certificate.pdf", "application/pdf", "%PDF-1.7".getBytes()));
        return request;
    }
}
