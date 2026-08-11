package com.minet.sacco.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for Push Notification Subscription
 * Represents the subscription data received from the frontend
 */
public class PushSubscriptionDTO {

    private String endpoint;

    @JsonProperty("keys")
    private SubscriptionKeys keys;

    public PushSubscriptionDTO() {
    }

    public PushSubscriptionDTO(String endpoint, SubscriptionKeys keys) {
        this.endpoint = endpoint;
        this.keys = keys;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public SubscriptionKeys getKeys() {
        return keys;
    }

    public void setKeys(SubscriptionKeys keys) {
        this.keys = keys;
    }

    /**
     * Inner class representing the cryptographic keys for the subscription
     */
    public static class SubscriptionKeys {
        
        @JsonProperty("p256dh")
        private String p256dh;

        @JsonProperty("auth")
        private String auth;

        public SubscriptionKeys() {
        }

        public SubscriptionKeys(String p256dh, String auth) {
            this.p256dh = p256dh;
            this.auth = auth;
        }

        public String getP256dh() {
            return p256dh;
        }

        public void setP256dh(String p256dh) {
            this.p256dh = p256dh;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }

        @Override
        public String toString() {
            return "SubscriptionKeys{" +
                    "p256dh='" + (p256dh != null ? p256dh.substring(0, Math.min(10, p256dh.length())) + "..." : "null") + '\'' +
                    ", auth='" + (auth != null ? auth.substring(0, Math.min(10, auth.length())) + "..." : "null") + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "PushSubscriptionDTO{" +
                "endpoint='" + endpoint + '\'' +
                ", keys=" + keys +
                '}';
    }
}
