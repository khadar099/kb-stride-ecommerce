
package com.khadar.welcomepage;

import com.khadar.welcomepage.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class WelcomeController {

    private static final Logger logger =
            LoggerFactory.getLogger(WelcomeController.class);

    private static final String SIGNUP_VERIFIED_MOBILE =
            "SIGNUP_VERIFIED_MOBILE";

    private final UserService userService;
    private final OtpService otpService;

    public WelcomeController(
            UserService userService,
            OtpService otpService) {

        this.userService = userService;
        this.otpService = otpService;
    }

    // =====================================
    // WELCOME PAGE
    // =====================================

    @GetMapping("/")
    public String welcome() {
        return "welcome";
    }

    // =====================================
    // COMBINED AUTHENTICATION PAGE
    // =====================================

    @GetMapping("/auth")
    public String auth(
            @RequestParam(required = false) String mode,
            Model model) {

        model.addAttribute("mode", mode);

        return "auth";
    }

    // Redirect old login URL
    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/auth";
    }

    // Redirect old signup URL
    @GetMapping("/signup")
    public String signupPage() {
        return "redirect:/auth?mode=signup";
    }

    // =====================================
    // EMAIL LOGIN
    // =====================================

    @PostMapping("/login")
    public String loginUser(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        try {

            Optional<User> userOptional =
                    userService.authenticateByEmail(email, password);

            if (userOptional.isEmpty()) {

                model.addAttribute(
                        "error",
                        "Invalid email or password."
                );

                return "auth";
            }

            establishLoginSession(
                    userOptional.get(),
                    request,
                    response
            );

            logger.info("Customer authenticated using email.");

            return "redirect:/shop";

        } catch (Exception e) {

            logger.error("Email login failed.", e);

            model.addAttribute(
                    "error",
                    "Unable to login. Please try again."
            );

            return "auth";
        }
    }

    // =====================================
    // SEND OTP
    // =====================================

    @PostMapping("/send-otp")
    public String sendOtp(
            @RequestParam String mobileNumber,
            @RequestParam(defaultValue = "LOGIN") String purpose,
            HttpServletRequest request,
            Model model) {

        try {

            String mobile =
                    userService.normalizeMobile(mobileNumber);

            if (mobile.isBlank()) {

                model.addAttribute(
                        "error",
                        "Enter a valid 10-digit mobile number."
                );

                return "auth";
            }

            if (!purpose.equals("LOGIN")
                    && !purpose.equals("SIGNUP")) {

                model.addAttribute(
                        "error",
                        "Invalid OTP request."
                );

                return "auth";
            }

            // Login OTP should only be sent to registered customers.
            if ("LOGIN".equals(purpose)
                    && !userService.authenticateByMobileExists(mobile)) {

                model.addAttribute(
                        "error",
                        "No account found for this mobile number. Please sign up."
                );

                model.addAttribute("mode", "signup");

                return "auth";
            }

            // Signup OTP should only be sent for new mobile numbers.
            if ("SIGNUP".equals(purpose)
                    && userService.mobileExists(mobile)) {

                model.addAttribute(
                        "error",
                        "This mobile number is already registered. Please login."
                );

                return "auth";
            }

            otpService.sendOtp(mobile);

            HttpSession session = request.getSession(true);

            session.setAttribute("OTP_MOBILE", mobile);
            session.setAttribute("OTP_PURPOSE", purpose);

            model.addAttribute(
                    "success",
                    "OTP sent successfully. Please check your mobile."
            );

            model.addAttribute("mobileNumber", mobile);
            model.addAttribute("otpSent", true);
            model.addAttribute("mode",
                    "SIGNUP".equals(purpose) ? "signup" : "login");

            return "auth";

        } catch (Exception e) {

            logger.warn("OTP request failed: {}", e.getMessage());

            model.addAttribute(
                    "error",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Unable to send OTP. Please try again."
            );

            return "auth";
        }
    }

    // =====================================
    // VERIFY OTP
    // =====================================

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam String mobileNumber,
            @RequestParam String otp,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        try {

            String mobile =
                    userService.normalizeMobile(mobileNumber);

            HttpSession session = request.getSession(false);

            if (session == null) {

                model.addAttribute(
                        "error",
                        "Please request a new OTP."
                );

                return "auth";
            }

            String requestedMobile =
                    (String) session.getAttribute("OTP_MOBILE");

            String purpose =
                    (String) session.getAttribute("OTP_PURPOSE");

            if (requestedMobile == null
                    || !requestedMobile.equals(mobile)
                    || purpose == null) {

                model.addAttribute(
                        "error",
                        "Please request a new OTP for this mobile number."
                );

                return "auth";
            }

            if (!otpService.verifyOtp(mobile, otp)) {

                model.addAttribute(
                        "error",
                        "Invalid or expired OTP. Please try again."
                );

                model.addAttribute("mobileNumber", mobile);
                model.addAttribute(
                        "mode",
                        "SIGNUP".equals(purpose) ? "signup" : "login"
                );

                return "auth";
            }

            // =================================
            // SIGNUP OTP VERIFICATION
            // =================================

            if ("SIGNUP".equals(purpose)) {

                session.setAttribute(
                        SIGNUP_VERIFIED_MOBILE,
                        mobile
                );

                session.removeAttribute("OTP_MOBILE");
                session.removeAttribute("OTP_PURPOSE");

                model.addAttribute(
                        "success",
                        "Mobile verified successfully. Complete your registration."
                );

                model.addAttribute("mobileVerified", true);
                model.addAttribute("mobileNumber", mobile);
                model.addAttribute("mode", "signup");

                return "auth";
            }

            // =================================
            // LOGIN OTP VERIFICATION
            // =================================

            Optional<User> userOptional =
                    userService.authenticateByMobile(mobile);

            if (userOptional.isEmpty()) {

                model.addAttribute(
                        "error",
                        "No registered account found. Please sign up."
                );

                model.addAttribute("mode", "signup");

                return "auth";
            }

            establishLoginSession(
                    userOptional.get(),
                    request,
                    response
            );

            otpService.clearOtp(mobile);

            session.removeAttribute("OTP_MOBILE");
            session.removeAttribute("OTP_PURPOSE");

            logger.info("Customer authenticated using mobile OTP.");

            return "redirect:/shop";

        } catch (Exception e) {

            logger.error("OTP verification failed.", e);

            model.addAttribute(
                    "error",
                    "OTP verification failed. Please try again."
            );

            return "auth";
        }
    }

    // =====================================
    // SIGNUP
    // =====================================

    @PostMapping("/signup")
    public String registerUser(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String mobileNumber,
            @RequestParam String password,
            HttpServletRequest request,
            Model model) {

        try {

            String mobile =
                    userService.normalizeMobile(mobileNumber);

            HttpSession session = request.getSession(false);

            String verifiedMobile = session == null
                    ? null
                    : (String) session.getAttribute(
                            SIGNUP_VERIFIED_MOBILE
                    );

            if (verifiedMobile == null
                    || mobile.isBlank()
                    || !verifiedMobile.equals(mobile)) {

                model.addAttribute(
                        "error",
                        "Please verify your mobile number using OTP before registering."
                );

                model.addAttribute("mode", "signup");

                return "auth";
            }

            String result = userService.registerUser(
                    fullName,
                    email,
                    mobile,
                    password,
                    true
            );

            if (!"SUCCESS".equals(result)) {

                String errorMessage;

                switch (result) {

                    case "INVALID_NAME":
                        errorMessage = "Enter a valid full name.";
                        break;

                    case "INVALID_EMAIL":
                        errorMessage = "Enter a valid email address.";
                        break;

                    case "INVALID_PASSWORD":
                        errorMessage = "Password must contain 8 to 72 characters.";
                        break;

                    case "EMAIL_EXISTS":
                        errorMessage = "This email is already registered.";
                        break;

                    case "MOBILE_EXISTS":
                        errorMessage = "This mobile number is already registered.";
                        break;

                    case "MOBILE_NOT_VERIFIED":
                        errorMessage = "Please verify your mobile number.";
                        break;

                    default:
                        errorMessage = "Registration failed. Please try again.";
                }

                model.addAttribute("error", errorMessage);
                model.addAttribute("mode", "signup");

                return "auth";
            }

            session.removeAttribute(SIGNUP_VERIFIED_MOBILE);

            otpService.clearOtp(mobile);

            logger.info("New customer registered successfully.");

            return "redirect:/auth?signupSuccess=true";

        } catch (Exception e) {

            logger.error("Registration failed.", e);

            model.addAttribute(
                    "error",
                    "Unable to register. Please try again."
            );

            model.addAttribute("mode", "signup");

            return "auth";
        }
    }

    // =====================================
    // CREATE SPRING SECURITY SESSION
    // =====================================

    private void establishLoginSession(
            User user,
            HttpServletRequest request,
            HttpServletResponse response) {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_USER")
                        )
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        HttpSessionSecurityContextRepository repository =
                new HttpSessionSecurityContextRepository();

        repository.saveContext(
                context,
                request,
                response
        );
    }

    // =====================================
    // SHOP PAGE
    // =====================================

    @GetMapping("/shop")
    public String shop() {
        return "shop";
    }

    // =====================================
    // TRACK ORDER PAGE
    // =====================================

    @GetMapping("/track-order")
    public String trackOrder(
            @RequestParam(required = false) String orderId,
            Model model) {

        if (orderId != null && !orderId.isBlank()) {

            model.addAttribute(
                    "message",
                    "Order tracking is not connected yet. Please check back later."
            );
        }

        return "track-order";
    }
}
