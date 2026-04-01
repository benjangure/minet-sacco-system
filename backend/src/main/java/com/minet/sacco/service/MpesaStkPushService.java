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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class MpesaStkPushService {

    @Autowired
    private MpesaConfig mpesaConfig;

    @Autowired
    private MpesaAuthService mpesaAuthService;

    /**
     * Initiate STK Push for M-Pesa deposit
     * This prompts the user to enter their M-Pesa PIN
     */
    public JsonObject initiateStkPush(String phoneNumber, long amount) {
        try {
            String accessToken = mpesaAuthService.getAccessToken();
            
            // Generate timestamp
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String timestamp = now.format(formatter);

            // Generate password: Base64(BusinessShortCode + Passkey + Timestamp)
            String passwordString = mpesaConfig.getBusinessShortCode() + 
                                   mpesaConfig.getPasskey() + 
                                   timestamp;
            String password = Base64.getEncoder().encodeToString(passwordString.getBytes());

            // Build request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("BusinessShortCode", mpesaConfig.getBusinessShortCode());
            requestBody.addProperty("Password", password);
            requestBody.addProperty("Timestamp", timestamp);
            requestBody.addProperty("TransactionType", "CustomerPayBillOnline");
            requestBody.addProperty("Amount", amount);
            requestBody.addProperty("PartyA", phoneNumber);
            requestBody.addProperty("PartyB", mpesaConfig.getBusinessShortCode());
            requestBody.addProperty("PhoneNumber", phoneNumber);
            requestBody.addProperty("CallBackURL", mpesaConfig.getCallbackUrl());
            requestBody.addProperty("AccountReference", "MINET-DEPOSIT-" + System.currentTimeMillis());
            requestBody.addProperty("TransactionDesc", "Deposit to Minet SACCO");

            // Make API call
            HttpClient httpClient = HttpClients.createDefault();
            String url = mpesaConfig.getBaseUrl() + "/mpesa/stkpush/v1/processrequest";
            
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
            errorResponse.addProperty("message", "Error initiating STK Push: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Query STK Push status
     */
    public JsonObject queryStkPushStatus(String checkoutRequestId) {
        try {
            String accessToken = mpesaAuthService.getAccessToken();

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String timestamp = now.format(formatter);

            String passwordString = mpesaConfig.getBusinessShortCode() + 
                                   mpesaConfig.getPasskey() + 
                                   timestamp;
            String password = Base64.getEncoder().encodeToString(passwordString.getBytes());

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("BusinessShortCode", mpesaConfig.getBusinessShortCode());
            requestBody.addProperty("Password", password);
            requestBody.addProperty("Timestamp", timestamp);
            requestBody.addProperty("CheckoutRequestID", checkoutRequestId);

            HttpClient httpClient = HttpClients.createDefault();
            String url = mpesaConfig.getBaseUrl() + "/mpesa/stkpushquery/v1/query";
            
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
            errorResponse.addProperty("message", "Error querying STK status: " + e.getMessage());
            return errorResponse;
        }
    }
}
