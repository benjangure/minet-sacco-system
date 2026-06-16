package com.minet.sacco.dto;

public class AuthResponse {

    private String token;
    private Long memberId;
    private Boolean firstLogin;

    public AuthResponse() {}

    public AuthResponse(String token) {
        this.token = token;
    }

    public AuthResponse(String token, Long memberId, Boolean firstLogin) {
        this.token = token;
        this.memberId = memberId;
        this.firstLogin = firstLogin;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Boolean getFirstLogin() { return firstLogin; }
    public void setFirstLogin(Boolean firstLogin) { this.firstLogin = firstLogin; }
}
