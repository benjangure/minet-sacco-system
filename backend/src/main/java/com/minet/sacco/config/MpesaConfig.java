package com.minet.sacco.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mpesa")
public class MpesaConfig {
    private String consumerKey;
    private String consumerSecret;
    private String businessShortCode;
    private String passkey;
    private String environment;
    private String callbackUrl;
    private String timeoutUrl;

    // Getters and Setters
    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public String getBusinessShortCode() {
        return businessShortCode;
    }

    public void setBusinessShortCode(String businessShortCode) {
        this.businessShortCode = businessShortCode;
    }

    public String getPasskey() {
        return passkey;
    }

    public void setPasskey(String passkey) {
        this.passkey = passkey;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getTimeoutUrl() {
        return timeoutUrl;
    }

    public void setTimeoutUrl(String timeoutUrl) {
        this.timeoutUrl = timeoutUrl;
    }

    public String getBaseUrl() {
        if ("production".equalsIgnoreCase(environment)) {
            return "https://api.safaricom.co.ke";
        }
        return "https://sandbox.safaricom.co.ke";
    }
}
