package com.fund.service;

public interface GoogleOAuthService {

    String getAuthorizationUrl(String state);

    String exchangeCodeForToken(String code);

    String getUserInfo(String accessToken);

    boolean isConfigured();
}
