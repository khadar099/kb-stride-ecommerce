
package com.khadar.welcomepage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Enter a valid 10-digit Indian mobile number"
    )
    private String mobileNumber;

    @Pattern(
        regexp = "^[0-9]{6}$",
        message = "OTP must contain exactly 6 digits"
    )
    private String otp;

    // Default constructor
    public OtpRequest() {
    }

    // Parameterized constructor
    public OtpRequest(String mobileNumber, String otp) {
        this.mobileNumber = mobileNumber;
        this.otp = otp;
    }

    // Getters and Setters

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
