
package com.khadar.welcomepage.service;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class Msg91Service {

    private static final Logger logger =
            LoggerFactory.getLogger(Msg91Service.class);

    private static final String SEND_OTP_URL =
            "https://control.msg91.com/api/v5/otp";

    private static final String VERIFY_OTP_URL =
            "https://control.msg91.com/api/v5/otp/verify";

    private final RestClient restClient;

    @Value("${app.sms.msg91.auth-key:}")
    private String authKey;

    @Value("${app.sms.msg91.template-id:}")
    private String templateId;

    @Value("${app.otp.expiry-seconds:300}")
    private int expirySeconds;

    public Msg91Service(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    // ==========================================
    // SEND OTP
    // ==========================================

    public boolean sendOtp(String mobileNumber) {

        validateConfiguration();

        String mobile = normalizeMobileNumber(mobileNumber);

        try {

            int expiryMinutes = Math.max(
                    1,
                    (int) Math.ceil(expirySeconds / 60.0)
            );

            String url = UriComponentsBuilder
                    .fromUriString(SEND_OTP_URL)
                    .queryParam("template_id", templateId)
                    .queryParam("mobile", mobile)
                    .queryParam("otp_length", 6)
                    .queryParam("otp_expiry", expiryMinutes)
                    .build()
                    .encode()
                    .toUriString();

            JsonNode response = restClient.post()
                    .uri(url)
                    .header("authkey", authKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(JsonNode.class);

            boolean success = isSuccess(response);

            if (success) {
                logger.info("MSG91 OTP request accepted.");
            } else {
                logger.warn("MSG91 rejected the OTP request.");
            }

            return success;

        } catch (Exception e) {

            logger.error(
                    "Error while sending OTP through MSG91: {}",
                    e.getMessage()
            );

            throw new IllegalStateException(
                    "Unable to send OTP. Please try again later."
            );
        }
    }

    // ==========================================
    // VERIFY OTP
    // ==========================================

    public boolean verifyOtp(
            String mobileNumber,
            String otp) {

        validateConfiguration();

        String mobile = normalizeMobileNumber(mobileNumber);

        if (otp == null || !otp.matches("^[0-9]{6}$")) {
            return false;
        }

        try {

            String url = UriComponentsBuilder
                    .fromUriString(VERIFY_OTP_URL)
                    .queryParam("mobile", mobile)
                    .queryParam("otp", otp)
                    .build()
                    .encode()
                    .toUriString();

            JsonNode response = restClient.get()
                    .uri(url)
                    .header("authkey", authKey)
                    .retrieve()
                    .body(JsonNode.class);

            boolean success = isSuccess(response);

            if (success) {
                logger.info("MSG91 OTP verification successful.");
            } else {
                logger.warn("MSG91 OTP verification failed.");
            }

            return success;

        } catch (Exception e) {

            logger.error(
                    "Error while verifying OTP through MSG91: {}",
                    e.getMessage()
            );

            throw new IllegalStateException(
                    "Unable to verify OTP. Please try again later."
            );
        }
    }

    // ==========================================
    // NORMALIZE INDIAN MOBILE NUMBER
    // ==========================================

    private String normalizeMobileNumber(String mobileNumber) {

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

        return "91" + mobile;
    }

    // ==========================================
    // CHECK MSG91 RESPONSE
    // ==========================================

    private boolean isSuccess(JsonNode response) {

        return response != null
                && response.has("type")
                && "success".equalsIgnoreCase(
                        response.get("type").asText()
                );
    }

    // ==========================================
    // CHECK CONFIGURATION
    // ==========================================

    private void validateConfiguration() {

        if (authKey == null || authKey.isBlank()) {
            throw new IllegalStateException(
                    "MSG91 Auth Key is not configured."
            );
        }

        if (templateId == null || templateId.isBlank()) {
            throw new IllegalStateException(
                    "MSG91 Template ID is not configured."
            );
        }
    }
}
