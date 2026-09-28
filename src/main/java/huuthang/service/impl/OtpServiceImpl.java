package huuthang.service.impl;

import huuthang.entity.OtpToken;
import huuthang.repository.OtpTokenRepository;
import huuthang.service.EmailService;
import huuthang.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_MINUTES = 5;
    private final OtpTokenRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    private String generateOtp() {
        return "%06d".formatted(random.nextInt(1_000_000));
    }

    private void send(String rawEmail, String type, String subject) {
        String email = rawEmail.trim().toLowerCase();
        repository.deleteByEmailAndType(email, type);
        String otp = generateOtp();
        OtpToken token = OtpToken.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(otp))
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_MINUTES))
                .attempts(0)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(token);
        emailService.sendOtp(email, otp, subject);
    }

    @Override
    @Transactional
    public void sendRegisterOtp(String email) {
        send(email, "REGISTER", "HUUTHANG SHOP - Xác nhận đăng ký tài khoản");
    }

    @Override
    @Transactional
    public void sendResetPasswordOtp(String email) {
        send(email, "RESET_PASSWORD", "HUUTHANG SHOP - OTP đặt lại mật khẩu");
    }

    private boolean verify(String rawEmail, String otp, String type) {
        String email = rawEmail.trim().toLowerCase();
        OtpToken token = repository
                .findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(email, type)
                .orElse(null);
        if (token == null
                || token.getExpiresAt().isBefore(LocalDateTime.now())
                || token.getAttempts() >= MAX_ATTEMPTS) {
            return false;
        }

        token.setAttempts(token.getAttempts() + 1);
        if (!passwordEncoder.matches(otp, token.getOtpHash())) {
            repository.save(token);
            return false;
        }
        token.setUsed(true);
        repository.save(token);
        return true;
    }

    @Override
    @Transactional
    public boolean verifyRegisterOtp(String email, String otp) {
        return verify(email, otp, "REGISTER");
    }

    @Override
    @Transactional
    public boolean verifyResetPasswordOtp(String email, String otp) {
        return verify(email, otp, "RESET_PASSWORD");
    }
}
