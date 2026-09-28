package huuthang.service;

import huuthang.dto.RegisterDTO;
import huuthang.entity.Role;
import huuthang.entity.User;
import huuthang.repository.RoleRepository;
import huuthang.repository.UserRepository;
import huuthang.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTests {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private OtpService otpService;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        otpService = mock(OtpService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AuthServiceImpl(userRepository, roleRepository, passwordEncoder, otpService);
    }

    @Test
    void registerCreatesDisabledUserWithBcryptPasswordAndSendsOtp() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newuser");
        dto.setEmail(" NewUser@Example.com ");
        dto.setFullName("Nguyễn Văn A");
        dto.setPassword("secret123");
        dto.setConfirmPassword("secret123");

        Role role = Role.builder().id(1L).name("ROLE_USER").build();
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);

        service.register(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertFalse(saved.isEnabled());
        assertTrue(passwordEncoder.matches("secret123", saved.getPassword()));
        assertTrue("newuser@example.com".equals(saved.getEmail()));
        verify(otpService).sendRegisterOtp("newuser@example.com");
    }

    @Test
    void verifyRegisterEnablesUserAfterValidOtp() {
        User user = User.builder().email("user@example.com").enabled(false).build();
        when(otpService.verifyRegisterOtp("user@example.com", "123456")).thenReturn(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertTrue(service.verifyRegister("user@example.com", "123456"));
        assertTrue(user.isEnabled());
    }
}
