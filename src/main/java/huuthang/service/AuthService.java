package huuthang.service;

import huuthang.dto.RegisterDTO;

public interface AuthService {
    void register(RegisterDTO dto);

    boolean verifyRegister(String email, String otp);

    void resendRegisterOtp(String email);

    void forgotPassword(String email);

    boolean verifyResetOtp(String email, String otp);

    void resetPassword(String email, String password);
}
