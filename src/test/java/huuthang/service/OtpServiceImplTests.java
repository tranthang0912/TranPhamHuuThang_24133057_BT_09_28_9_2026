package huuthang.service;

import huuthang.entity.OtpToken;
import huuthang.repository.OtpTokenRepository;
import huuthang.service.impl.OtpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceImplTests {

    private OtpTokenRepository repository;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private OtpServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(OtpTokenRepository.class);
        emailService = mock(EmailService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new OtpServiceImpl(repository, passwordEncoder, emailService);
    }

    @Test
    void sendRegisterOtpStoresOnlyHashAndSendsSixDigits() {
        service.sendRegisterOtp(" USER@example.com ");

        ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).deleteByEmailAndType("user@example.com", "REGISTER");
        verify(repository).save(tokenCaptor.capture());
        verify(emailService).sendOtp(eq("user@example.com"), otpCaptor.capture(), any(String.class));

        OtpToken token = tokenCaptor.getValue();
        String plainOtp = otpCaptor.getValue();
        assertTrue(plainOtp.matches("\\d{6}"));
        assertNotEquals(plainOtp, token.getOtpHash());
        assertTrue(passwordEncoder.matches(plainOtp, token.getOtpHash()));
        assertEquals("REGISTER", token.getType());
        assertFalse(token.isUsed());
    }

    @Test
    void verifyRegisterOtpMarksValidTokenAsUsed() {
        OtpToken token = OtpToken.builder()
                .email("user@example.com")
                .type("REGISTER")
                .otpHash(passwordEncoder.encode("123456"))
                .expiresAt(LocalDateTime.now().plusMinutes(1))
                .createdAt(LocalDateTime.now())
                .attempts(0)
                .used(false)
                .build();
        when(repository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(
                "user@example.com", "REGISTER")).thenReturn(Optional.of(token));

        assertTrue(service.verifyRegisterOtp("user@example.com", "123456"));
        assertTrue(token.isUsed());
        assertEquals(1, token.getAttempts());
        verify(repository).save(token);
    }

    @Test
    void verifyRegisterOtpRejectsExpiredToken() {
        OtpToken token = OtpToken.builder()
                .email("user@example.com")
                .type("REGISTER")
                .otpHash(passwordEncoder.encode("123456"))
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .createdAt(LocalDateTime.now().minusMinutes(6))
                .attempts(0)
                .used(false)
                .build();
        when(repository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(
                "user@example.com", "REGISTER")).thenReturn(Optional.of(token));

        assertFalse(service.verifyRegisterOtp("user@example.com", "123456"));
    }
}
