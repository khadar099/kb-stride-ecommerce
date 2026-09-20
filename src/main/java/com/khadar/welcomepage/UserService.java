package com.khadar.welcomepage;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Logger logger =
            LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // ==========================
    // SIGNUP
    // ==========================
    public String registerUser(
            String fullName,
            String email,
            String password) {

        fullName = fullName == null ? "" : fullName.trim();
        email = email == null ? "" : email.trim().toLowerCase();
        password = password == null ? "" : password;

        // Validate email
        if (!EMAIL_PATTERN.matcher(email).matches()) {

            logger.warn(
                    "Signup failed - invalid email format: {}",
                    email
            );

            return "INVALID_EMAIL";
        }

        // Check if email already exists
        if (userRepository.existsByEmailIgnoreCase(email)) {

            logger.warn(
                    "Signup failed - email already registered: {}",
                    email
            );

            return "EMAIL_EXISTS";
        }

        // Create new user
        User user = new User();

        user.setFullName(fullName);
        user.setEmail(email);

        // Encrypt password before saving
        String encryptedPassword =
                passwordEncoder.encode(password);

        user.setPassword(encryptedPassword);

        // Save user
        userRepository.save(user);

        logger.info(
                "User registration successful - email: {}",
                email
        );

        return "SUCCESS";
    }

    // ==========================
    // LOGIN
    // ==========================
    public String loginUser(
            String email,
            String password) {

        email = email == null ? "" : email.trim().toLowerCase();
        password = password == null ? "" : password;

        Optional<User> userOptional =
                userRepository.findByEmailIgnoreCase(email);

        // User does not exist
        if (userOptional.isEmpty()) {

            logger.warn(
                    "Login failed - user not registered - email: {}",
                    email
            );

            return "USER_NOT_REGISTERED";
        }

        User user = userOptional.get();

        // User exists, but password is incorrect
        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            logger.warn(
                    "Login failed - invalid password - email: {}",
                    email
            );

            return "INVALID_PASSWORD";
        }

        // Login successful
        logger.info(
                "Login successful - email: {}",
                email
        );

        return "SUCCESS";
    }
}
