package com.fund.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fund.service.GoogleOAuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri:http://localhost:8080/api/auth/google/callback}")
    private String redirectUri;

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Override
    public String getAuthorizationUrl(String state) {
        return AUTH_URL + "?client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&response_type=code"
                + "&scope=" + urlEncode("openid email profile")
                + "&state=" + urlEncode(state)
                + "&access_type=offline";
    }

    @Override
    public String exchangeCodeForToken(String code) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(TOKEN_URL);
            httpPost.setHeader("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("code", code);
            body.put("client_id", clientId);
            body.put("client_secret", clientSecret);
            body.put("redirect_uri", redirectUri);
            body.put("grant_type", "authorization_code");
            httpPost.setEntity(new StringEntity(body.toJSONString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject json = JSON.parseObject(responseBody);
                return json.getString("access_token");
            }
        } catch (Exception e) {
            log.error("Exchange code for token failed", e);
            throw new RuntimeException("Google OAuth token exchange failed", e);
        }
    }

    @Override
    public String getUserInfo(String accessToken) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(USERINFO_URL);
            httpGet.setHeader("Authorization", "Bearer " + accessToken);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            log.error("Get Google user info failed", e);
            throw new RuntimeException("Failed to get Google user info", e);
        }
    }

    @Override
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
