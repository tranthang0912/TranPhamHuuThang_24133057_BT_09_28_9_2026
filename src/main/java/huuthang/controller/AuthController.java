package huuthang.controller;

import huuthang.dto.ForgotPasswordDTO;
import huuthang.dto.RegisterDTO;
import huuthang.dto.ResetPasswordDTO;
import huuthang.dto.VerifyOtpDTO;
import huuthang.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    String register(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    String register(
            @Valid @ModelAttribute RegisterDTO registerDTO,
            BindingResult result,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(registerDTO);
            redirect.addFlashAttribute("success", "OTP đã được gửi đến email.");
            redirect.addAttribute("email", registerDTO.getEmail());
            return "redirect:/verify-otp";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject("register.error", exception.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    String verifyPage(@RequestParam(required = false) String email, Model model) {
        VerifyOtpDTO dto = new VerifyOtpDTO();
        dto.setEmail(email);
        model.addAttribute("verifyOtpDTO", dto);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    String verify(
            @Valid @ModelAttribute VerifyOtpDTO verifyOtpDTO,
            BindingResult result,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "auth/verify-otp";
        }
        if (!authService.verifyRegister(verifyOtpDTO.getEmail(), verifyOtpDTO.getOtp())) {
            result.reject("otp.error", "OTP không hợp lệ, hết hạn hoặc đã quá số lần thử.");
            return "auth/verify-otp";
        }
        redirect.addFlashAttribute("success", "Xác nhận thành công. Hãy đăng nhập.");
        return "redirect:/login";
    }

    @PostMapping("/resend-register-otp")
    String resend(@RequestParam String email, RedirectAttributes redirect) {
        try {
            authService.resendRegisterOtp(email);
            redirect.addFlashAttribute("success", "Đã gửi lại OTP.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        redirect.addAttribute("email", email);
        return "redirect:/verify-otp";
    }

    @GetMapping("/forgot-password")
    String forgot(Model model) {
        model.addAttribute("forgotPasswordDTO", new ForgotPasswordDTO());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    String forgot(
            @Valid @ModelAttribute ForgotPasswordDTO forgotPasswordDTO,
            BindingResult result,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "auth/forgot-password";
        }
        try {
            authService.forgotPassword(forgotPasswordDTO.getEmail());
            redirect.addFlashAttribute("email", forgotPasswordDTO.getEmail());
            redirect.addFlashAttribute("success", "OTP đặt lại mật khẩu đã được gửi.");
            return "redirect:/reset-password";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject("forgot.error", exception.getMessage());
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    String reset(Model model) {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        Object email = model.asMap().get("email");
        if (email != null) {
            dto.setEmail(email.toString());
        }
        model.addAttribute("resetPasswordDTO", dto);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    String reset(
            @Valid @ModelAttribute ResetPasswordDTO resetPasswordDTO,
            BindingResult result,
            @RequestParam String otp,
            RedirectAttributes redirect) {
        if (!Objects.equals(resetPasswordDTO.getPassword(), resetPasswordDTO.getConfirmPassword())) {
            result.reject("password.error", "Mật khẩu xác nhận không đúng.");
        }
        if (result.hasErrors()) {
            return "auth/reset-password";
        }
        if (!authService.verifyResetOtp(resetPasswordDTO.getEmail(), otp)) {
            result.reject("otp.error", "OTP không hợp lệ hoặc đã hết hạn.");
            return "auth/reset-password";
        }
        authService.resetPassword(resetPasswordDTO.getEmail(), resetPasswordDTO.getPassword());
        redirect.addFlashAttribute("success", "Đổi mật khẩu thành công.");
        return "redirect:/login";
    }
}
