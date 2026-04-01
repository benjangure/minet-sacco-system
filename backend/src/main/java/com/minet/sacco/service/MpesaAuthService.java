package com.minet.sacco.service;

import com.minet.sacco.config.MpesaConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class MpesaAuthService {

    @Autowired
    private MpesaConfig mpesaConfig;

    private String cachedToken;
    private long tokenExpiryTime;

    /**
     * Get OAuth token from Daraja API
     */
    public String getAccessToken() {
        // Return cached token if still valid
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }

        try {
            String consumerKey = mpesaConfig.getConsumerKey();
            String consumerSecret = mpesaConfig.getConsumerSecret();
            
            System.out.println("DEBUG: Consumer Key: " + (consumerKey != null ? consumerKey.substring(0, Math.min(10, consumerKey.length())) + "..." : "NULL"));
            System.out.println("DEBUG: Consumer Secret: " + (consumerSecret != null ? consumerSecret.substring(0, Math.min(10, consumerSecret.length())) + "..." : "NULL"));
            System.out.println("DEBUG: Base URL: " + mpesaConfig.getBaseUrl());
            
            if (consumerKey == null || consumerKey.isEmpty() || consumerSecret == null || consumerSecret.isEmpty()) {
                throw new RuntimeException("M-Pesa credentials not configured. Check environment variables.");
            }
            
            String auth = consumerKey + ":" + consumerSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpClient httpClient = HttpClients.createDefault();
            String url = mpesaConfig.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials";
            
            System.out.println("DEBUG: Auth URL: " + url);
            
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", "Basic " + encodedAuth);

            String responseBody = httpClient.execute(httpGet, response -> {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            });

            System.out.println("DEBUG: Auth Response: " + responseBody);
            
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            if (jsonResponse.has("access_token")) {
                cachedToken = jsonResponse.get("access_token").getAsString();
                long expiresIn = jsonResponse.get("expires_in").getAsLong();
                tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000) - 60000; // Refresh 1 min before expiry
                return cachedToken;
            } else {
                throw new RuntimeException("Failed to get access token: " + responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting M-Pesa access token: " + e.getMessage(), e);
        }
    }
}
