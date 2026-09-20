package com.khadar.welcomepage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WelcomeController {

    private static final Logger logger =
            LoggerFactory.getLogger(WelcomeController.class);

    private final UserService userService;

    public WelcomeController(UserService userService) {
        this.userService = userService;
    }

    // ==========================
    // WELCOME PAGE
    // ==========================
    @GetMapping("/")
    public String welcome() {

        logger.info("Welcome page requested");

        return "welcome";
    }

    // ==========================
    // LOGIN PAGE
    // ==========================
    @GetMapping("/login")
    public String login() {

        logger.info("Login page requested");

        return "login";
    }

    // ==========================
    // LOGIN
    // ==========================
    @PostMapping("/login")
    public String loginUser(
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        logger.info(
                "Login request received - email: {}",
                email
        );

        try {

            String result = userService.loginUser(
                    email,
                    password
            );

            // Login successful
            if ("SUCCESS".equals(result)) {

                logger.info(
                        "Login request successful - email: {}",
                        email
                );

                return "redirect:/shop";
            }

            // User is not registered
            if ("USER_NOT_REGISTERED".equals(result)) {

                logger.warn(
                        "Login request failed - user not registered - email: {}",
                        email
                );

                model.addAttribute(
                        "error",
                        "User is not registered. Please sign up first."
                );

                return "login";
            }

            // Wrong password
            if ("INVALID_PASSWORD".equals(result)) {

                logger.warn(
                        "Login request failed - invalid password - email: {}",
                        email
                );

                model.addAttribute(
                        "error",
                        "Invalid password. Please try again."
                );

                return "login";
            }

            // Unexpected result
            logger.error(
                    "Login request returned unexpected result - email: {} result: {}",
                    email,
                    result
            );

            model.addAttribute(
                    "error",
                    "Something went wrong. Please try again."
            );

            return "login";

        } catch (Exception e) {

            logger.error(
                    "Unexpected error during login - email: {}",
                    email,
                    e
            );

            model.addAttribute(
                    "error",
                    "Something went wrong. Please try again."
            );

            return "login";
        }
    }

    // ==========================
    // SIGNUP PAGE
    // ==========================
    @GetMapping("/signup")
    public String signup() {

        logger.info("Signup page requested");

        return "signup";
    }

    // ==========================
    // SIGNUP
    // ==========================
    @PostMapping("/signup")
    public String registerUser(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        logger.info(
                "Signup request received - email: {}",
                email
        );

        try {

            String result = userService.registerUser(
                    fullName,
                    email,
                    password
            );

            // Invalid email
            if ("INVALID_EMAIL".equals(result)) {

                logger.warn(
                        "Signup request failed - invalid email - email: {}",
                        email
                );

                model.addAttribute(
                        "error",
                        "Please enter a correct email address."
                );

                return "signup";
            }

            // Email already registered
            if ("EMAIL_EXISTS".equals(result)) {

                logger.warn(
                        "Signup request failed - email already registered - email: {}",
                        email
                );

                model.addAttribute(
                        "error",
                        "Email is already registered."
                );

                return "signup";
            }

            // Signup successful
            if ("SUCCESS".equals(result)) {

                logger.info(
                        "Signup request successful - email: {}",
                        email
                );

                return "redirect:/login?signupSuccess=true";
            }

            // Unexpected result
            logger.error(
                    "Signup request returned unexpected result - email: {} result: {}",
                    email,
                    result
            );

            model.addAttribute(
                    "error",
                    "Something went wrong. Please try again."
            );

            return "signup";

        } catch (Exception e) {

            logger.error(
                    "Unexpected error during signup - email: {}",
                    email,
                    e
            );

            model.addAttribute(
                    "error",
                    "Something went wrong. Please try again."
            );

            return "signup";
        }
    }

    // ==========================
    // SHOP PAGE
    // ==========================
    @GetMapping("/shop")
    public String shop() {

        logger.info("Shop page requested");

        return "shop";
    }

    // ==========================
    // TRACK ORDER
    // ==========================
    @GetMapping("/track-order")
    public String trackOrder() {

        logger.info("Track order page requested");

        return "track-order";
    }
}
