package com.minet.sacco.service;

import com.minet.sacco.config.MpesaConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MpesaB2CService {

    @Autowired
    private MpesaConfig mpesaConfig;

    @Autowired
    private MpesaAuthService mpesaAuthService;

    /**
     * Send B2C payment (Business to Customer)
     * Used for withdrawals - sends money from organization to member's M-Pesa
     */
    public JsonObject sendB2CPayment(String phoneNumber, long amount, String commandId) {
        try {
            String accessToken = mpesaAuthService.getAccessToken();

            // Build request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("OriginatorConversationID", "MINET-" + System.currentTimeMillis());
            requestBody.addProperty("InitiatorName", "minet_sacco");
            requestBody.addProperty("SecurityCredential", getSecurityCredential());
            requestBody.addProperty("CommandID", commandId); // "BusinessPayment" or "SalaryPayment"
            requestBody.addProperty("Amount", amount);
            requestBody.addProperty("PartyA", mpesaConfig.getBusinessShortCode());
            requestBody.addProperty("PartyB", phoneNumber);
            requestBody.addProperty("Remarks", "Withdrawal from Minet SACCO");
            requestBody.addProperty("QueueTimeOutURL", mpesaConfig.getTimeoutUrl());
            requestBody.addProperty("ResultURL", mpesaConfig.getCallbackUrl());
            requestBody.addProperty("Occasion", "Withdrawal");

            // Make API call
            HttpClient httpClient = HttpClients.createDefault();
            String url = mpesaConfig.getBaseUrl() + "/mpesa/b2c/v1/paymentrequest";
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + accessToken);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(requestBody.toString()));

            String responseBody = httpClient.execute(httpPost, response -> {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            });

            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            return jsonResponse;

        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", true);
            errorResponse.addProperty("message", "Error sending B2C payment: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Get security credential (encrypted initiator password)
     * For sandbox, use: "Safaricom124!"
     * For production, you need to encrypt your actual password
     */
    private String getSecurityCredential() {
        // TODO: Implement proper encryption for production
        // For now, return sandbox credential
        if ("sandbox".equalsIgnoreCase(mpesaConfig.getEnvironment())) {
            return "Safaricom124!";
        }
        // In production, encrypt the actual password using Safaricom's public certificate
        return "YOUR_ENCRYPTED_CREDENTIAL";
    }
}
