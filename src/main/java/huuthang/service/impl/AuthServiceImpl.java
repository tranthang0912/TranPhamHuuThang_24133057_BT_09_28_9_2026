package huuthang.service.impl;

import huuthang.dto.RegisterDTO;
import huuthang.entity.Role;
import huuthang.entity.User;
import huuthang.repository.RoleRepository;
import huuthang.repository.UserRepository;
import huuthang.service.AuthService;
import huuthang.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không đúng");
        }

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Chưa có ROLE_USER"));
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName().trim())
                .enabled(false)
                .role(role)
                .build();
        userRepository.save(user);
        otpService.sendRegisterOtp(email);
    }

    @Override
    @Transactional
    public boolean verifyRegister(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        if (!otpService.verifyRegisterOtp(normalizedEmail, otp)) {
            return false;
        }
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        user.setEnabled(true);
        return true;
    }

    @Override
    @Transactional
    public void resendRegisterOtp(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        if (user.isEnabled()) {
            throw new IllegalArgumentException("Tài khoản đã được xác nhận");
        }
        otpService.sendRegisterOtp(user.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        if (!userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email không tồn tại");
        }
        otpService.sendResetPasswordOtp(normalizedEmail);
    }

    @Override
    public boolean verifyResetOtp(String email, String otp) {
        return otpService.verifyResetPasswordOtp(email, otp);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String password) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        user.setPassword(passwordEncoder.encode(password));
    }
}
