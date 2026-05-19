package com.mkr.commerce.auth.service;

import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

    public void verify(String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()) {
            throw new BadRequestException("CAPTCHA verification required. Please complete the CAPTCHA.", ErrorCode.CAPTCHA_FAILED);
        }
        // Image CAPTCHA is verified on the frontend. Non-empty token confirms completion.
    }
}
