
package com.khadar.welcomepage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Logger logger =
            LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            );

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^.{2,100}$");

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================
    // REGISTER USER - NO OTP REQUIRED
    // =====================================

    @Transactional
    public String registerUser(
            String fullName,
            String email,
            String mobileNumber,
            String password,
            boolean mobileVerified) {

        fullName = fullName == null ? "" : fullName.trim();
        email = normalizeEmail(email);
        mobileNumber = normalizeMobile(mobileNumber);

        if (!NAME_PATTERN.matcher(fullName).matches()) {
            return "INVALID_NAME";
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "INVALID_EMAIL";
        }

        if (mobileNumber.isBlank()) {
            return "INVALID_MOBILE";
        }

        if (password == null || password.length() < 8
                || password.length() > 72) {
            return "INVALID_PASSWORD";
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return "EMAIL_EXISTS";
        }

        if (userRepository.existsByMobileNumber(mobileNumber)) {
            return "MOBILE_EXISTS";
        }

        User user = new User();

        user.setFullName(fullName);
        user.setEmail(email);
        user.setMobileNumber(mobileNumber);
        user.setPassword(passwordEncoder.encode(password));

        // Signup does not require OTP verification.
        user.setMobileVerified(false);

        try {
            userRepository.saveAndFlush(user);

            logger.info("Customer registration successful.");

            return "SUCCESS";

        } catch (
                org.springframework.dao.DataIntegrityViolationException e) {

            logger.warn("Registration rejected due to duplicate data.");

            return "REGISTRATION_CONFLICT";
        }
    }

    // =====================================
    // EMAIL AND PASSWORD LOGIN
    // =====================================

    @Transactional(readOnly = true)
    public Optional<User> authenticateByEmail(
            String email,
            String password) {

        email = normalizeEmail(email);

        if (!EMAIL_PATTERN.matcher(email).matches()
                || password == null) {
            return Optional.empty();
        }

        Optional<User> userOptional =
                userRepository.findByEmailIgnoreCase(email);

        if (userOptional.isEmpty()) {
            logger.warn("Email login failed.");
            return Optional.empty();
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            logger.warn("Password verification failed.");

            return Optional.empty();
        }

        logger.info("Email login successful.");

        return Optional.of(user);
    }

    // =====================================
    // FIND REGISTERED MOBILE
    // =====================================

    @Transactional(readOnly = true)
    public Optional<User> findByMobile(String mobileNumber) {

        String mobile = normalizeMobile(mobileNumber);

        if (mobile.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByMobileNumber(mobile);
    }

    // =====================================
    // MOBILE LOGIN - VERIFIED USERS
    // =====================================

    @Transactional(readOnly = true)
    public Optional<User> authenticateByMobile(
            String mobileNumber) {

        String mobile = normalizeMobile(mobileNumber);

        if (mobile.isBlank()) {
            return Optional.empty();
        }

        return userRepository
                .findByMobileNumberAndMobileVerifiedTrue(mobile);
    }

    // =====================================
    // CHECK MOBILE EXISTS
    // =====================================

    public boolean mobileExists(String mobileNumber) {

        String mobile = normalizeMobile(mobileNumber);

        return !mobile.isBlank()
                && userRepository.existsByMobileNumber(mobile);
    }

    // =====================================
    // CHECK REGISTERED AND VERIFIED MOBILE
    // =====================================

    public boolean authenticateByMobileExists(
            String mobileNumber) {

        String mobile = normalizeMobile(mobileNumber);

        return !mobile.isBlank()
                && userRepository
                    .findByMobileNumberAndMobileVerifiedTrue(mobile)
                    .isPresent();
    }

    // =====================================
    // NORMALIZE EMAIL
    // =====================================

    public String normalizeEmail(String email) {

        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }

    // =====================================
    // NORMALIZE MOBILE
    // =====================================

    public String normalizeMobile(String mobileNumber) {

        if (mobileNumber == null) {
            return "";
        }

        String mobile =
                mobileNumber.replaceAll("[\\s-]", "");

        if (mobile.startsWith("+91")) {
            mobile = mobile.substring(3);
        } else if (mobile.startsWith("91")
                && mobile.length() == 12) {
            mobile = mobile.substring(2);
        }

        if (!mobile.matches("^[6-9][0-9]{9}$")) {
            return "";
        }

        return mobile;
    }
}
