
package com.khadar.welcomepage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OtpService {

    private final Msg91Service msg91Service;

    private final ConcurrentMap<String, OtpState> otpStates =
            new ConcurrentHashMap<>();

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    public OtpService(Msg91Service msg91Service) {
        this.msg91Service = msg91Service;
    }

    // =====================================
    // SEND OTP
    // =====================================

    public void sendOtp(String mobileNumber) {

        String mobile = normalizeMobile(mobileNumber);

        long now = System.currentTimeMillis();

        OtpState state = otpStates.computeIfAbsent(
                mobile,
                key -> new OtpState()
        );

        synchronized (state) {

            if (state.lastSentAt > 0) {

                long elapsed =
                        (now - state.lastSentAt) / 1000;

                if (elapsed < resendCooldownSeconds) {

                    throw new IllegalStateException(
                            "Please wait before requesting another OTP."
                    );
                }
            }

            boolean sent = msg91Service.sendOtp(mobile);

            if (!sent) {

                throw new IllegalStateException(
                        "Unable to send OTP. Please try again."
                );
            }

            state.lastSentAt = now;
            state.attempts = 0;
            state.verified = false;
        }
    }

    // =====================================
    // VERIFY OTP
    // =====================================

    public boolean verifyOtp(
            String mobileNumber,
            String otp) {

        String mobile = normalizeMobile(mobileNumber);

        if (otp == null || !otp.matches("^[0-9]{6}$")) {
            return false;
        }

        OtpState state = otpStates.get(mobile);

        if (state == null) {
            return false;
        }

        synchronized (state) {

            if (state.verified) {
                return true;
            }

            if (state.lastSentAt == 0) {
                return false;
            }

            long elapsed =
                    (System.currentTimeMillis() - state.lastSentAt)
                            / 1000;

            // OTP validity: 5 minutes
            if (elapsed > 300) {
                otpStates.remove(mobile, state);
                return false;
            }

            if (state.attempts >= maxAttempts) {
                otpStates.remove(mobile, state);
                return false;
            }

            state.attempts++;

            boolean valid =
                    msg91Service.verifyOtp(mobile, otp);

            if (valid) {
                state.verified = true;
                return true;
            }

            if (state.attempts >= maxAttempts) {
                otpStates.remove(mobile, state);
            }

            return false;
        }
    }

    // =====================================
    // CHECK VERIFIED MOBILE
    // =====================================

    public boolean isVerified(String mobileNumber) {

        String mobile = normalizeMobile(mobileNumber);

        OtpState state = otpStates.get(mobile);

        if (state == null) {
            return false;
        }

        synchronized (state) {

            if (!state.verified) {
                return false;
            }

            long elapsed =
                    (System.currentTimeMillis() - state.lastSentAt)
                            / 1000;

            return elapsed <= 300;
        }
    }

    // =====================================
    // CLEAR OTP STATE
    // =====================================

    public void clearOtp(String mobileNumber) {

        String mobile = normalizeMobile(mobileNumber);

        otpStates.remove(mobile);
    }

    // =====================================
    // NORMALIZE MOBILE NUMBER
    // =====================================

    private String normalizeMobile(String mobileNumber) {

        if (mobileNumber == null) {
            throw new IllegalArgumentException(
                    "Mobile number is required."
            );
        }

        String mobile = mobileNumber.replaceAll("[\\s-]", "");

        if (mobile.startsWith("+91")) {
            mobile = mobile.substring(3);
        } else if (mobile.startsWith("91")
                && mobile.length() == 12) {
            mobile = mobile.substring(2);
        }

        if (!mobile.matches("^[6-9][0-9]{9}$")) {

            throw new IllegalArgumentException(
                    "Enter a valid 10-digit Indian mobile number."
            );
        }

        return mobile;
    }

    // =====================================
    // OTP STATE
    // =====================================

    private static class OtpState {

        private long lastSentAt = 0;

        private int attempts = 0;

        private boolean verified = false;
    }
}
