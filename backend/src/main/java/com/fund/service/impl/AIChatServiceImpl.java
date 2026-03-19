package com.fund.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.config.AIConfig;
import com.fund.dto.AIChatRequest;
import com.fund.entity.AIChatHistory;
import com.fund.entity.AIChatSession;
import com.fund.entity.Fund;
import com.fund.mapper.AIChatHistoryMapper;
import com.fund.mapper.AIChatSessionMapper;
import com.fund.service.AIChatService;
import com.fund.service.FundService;
import com.fund.vo.AIChatResponse;
import com.fund.vo.AIChatHistoryVO;
import com.fund.vo.AIChatSessionVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatServiceImpl implements AIChatService {

    private final AIConfig aiConfig;
    private final AIChatSessionMapper sessionMapper;
    private final AIChatHistoryMapper historyMapper;
    private final FundService fundService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public AIChatResponse chat(Long userId, AIChatRequest request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getMessage();

        if (sessionId == null || sessionId.isEmpty()) {
            AIChatSessionVO session = createSession(userId);
            sessionId = session.getSessionId();
        }

        AIChatHistory userHistory = new AIChatHistory();
        userHistory.setSessionId(sessionId);
        userHistory.setRole("user");
        userHistory.setContent(userMessage);
        userHistory.setCreateTime(LocalDateTime.now());
        historyMapper.insert(userHistory);

        ChatContext chatContext = buildChatContext(sessionId, userMessage);
        String aiResponse = callClaudeAPI(chatContext.getMessages(), chatContext.getSystemPrompt(), request.getModel());
        int tokensUsed = estimateTokens(userMessage) + estimateTokens(aiResponse);

        AIChatHistory aiHistory = new AIChatHistory();
        aiHistory.setSessionId(sessionId);
        aiHistory.setRole("assistant");
        aiHistory.setContent(aiResponse);
        aiHistory.setTokensUsed(tokensUsed);
        aiHistory.setCreateTime(LocalDateTime.now());
        historyMapper.insert(aiHistory);

        updateSession(sessionId, tokensUsed);
        updateSessionTitle(sessionId, userMessage);

        AIChatResponse response = new AIChatResponse();
        response.setSessionId(sessionId);
        response.setContent(aiResponse);
        response.setTokensUsed(tokensUsed);
        response.setCreateTime(LocalDateTime.now());

        return response;
    }

    @Override
    public List<AIChatSessionVO> getSessions(Long userId) {
        LambdaQueryWrapper<AIChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIChatSession::getUserId, userId)
                .orderByDesc(AIChatSession::getUpdateTime);
        List<AIChatSession> sessions = sessionMapper.selectList(wrapper);
        return sessions.stream().map(this::convertToSessionVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AIChatSessionVO createSession(Long userId) {
        AIChatSession session = new AIChatSession();
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setModelType(aiConfig.getDefaultModel());
        session.setTotalTokens(0);
        session.setStatus(1);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return convertToSessionVO(session);
    }

    @Override
    @Transactional
    public void deleteSession(Long userId, String sessionId) {
        AIChatSession session = sessionMapper.selectBySessionId(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("session not found or no permission");
        }

        LambdaQueryWrapper<AIChatHistory> historyWrapper = new LambdaQueryWrapper<>();
        historyWrapper.eq(AIChatHistory::getSessionId, sessionId);
        historyMapper.delete(historyWrapper);
        sessionMapper.deleteById(session.getId());
    }

    @Override
    public List<AIChatHistoryVO> getHistory(Long userId, String sessionId) {
        LambdaQueryWrapper<AIChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIChatHistory::getSessionId, sessionId)
                .orderByAsc(AIChatHistory::getCreateTime);
        List<AIChatHistory> historyList = historyMapper.selectList(wrapper);
        return historyList.stream().map(this::convertToHistoryVO).collect(Collectors.toList());
    }

    /**
     * 调用 Claude API
     */
    private String callClaudeAPI(List<Map<String, String>> messages, String systemPrompt, String model) {
        try {
            // 检查 API Key 是否配置
            if (aiConfig.getApiKey() == null || aiConfig.getApiKey().isEmpty()) {
                log.error("Claude API Key not configured");
                return "AI 助手未配置，请联系管理员配置 Claude API Key。";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", aiConfig.getApiKey());
            headers.set("anthropic-version", aiConfig.getApiVersion());

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model != null ? model : aiConfig.getDefaultModel());
            requestBody.put("max_tokens", aiConfig.getMaxTokens());
            requestBody.put("system", systemPrompt);

            // 构建 messages 数组（不包含 system）
            JSONArray messagesArray = new JSONArray();
            for (Map<String, String> msg : messages) {
                // 跳过 system 角色
                if ("system".equals(msg.get("role"))) {
                    continue;
                }

                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.get("role"));
                msgObj.put("content", msg.get("content"));
                messagesArray.add(msgObj);
            }
            requestBody.put("messages", messagesArray);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

            log.debug("Calling Claude API: {}", requestBody.toJSONString());

            String endpoint = aiConfig.getBaseUrl() + "/v1/messages";
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject responseJson = JSON.parseObject(response.getBody());
                // Claude 响应格式: content[0].text
                JSONArray content = responseJson.getJSONArray("content");
                if (content != null && !content.isEmpty()) {
                    JSONObject firstContent = content.getJSONObject(0);
                    if ("text".equals(firstContent.getString("type"))) {
                        return firstContent.getString("text");
                    }
                }

                // 检查错误信息
                JSONObject error = responseJson.getJSONObject("error");
                if (error != null) {
                    log.error("Claude API error: {}", error.getString("message"));
                    return "AI 服务返回错误：" + error.getString("message");
                }
            }

            log.error("Claude API call failed: {}, body: {}", response.getStatusCode(), response.getBody());
            return "抱歉，AI 服务暂时不可用，请稍后再试。";

        } catch (Exception e) {
            log.error("Failed to call Claude API", e);
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                return "AI 助手认证失败，请检查 API Key 配置。";
            }
            return "抱歉，处理请求时发生错误，请稍后再试。";
        }
    }

    /**
     * 构建聊天上下文（返回 System Prompt 和 Messages 分离的结构）
     */
    private ChatContext buildChatContext(String sessionId, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 加载历史消息（最近 10 条）
        LambdaQueryWrapper<AIChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIChatHistory::getSessionId, sessionId)
                .orderByDesc(AIChatHistory::getCreateTime)
                .last("LIMIT 10");
        List<AIChatHistory> historyList = historyMapper.selectList(wrapper);

        Collections.reverse(historyList);
        for (AIChatHistory history : historyList) {
            Map<String, String> historyMsg = new HashMap<>();
            historyMsg.put("role", history.getRole());
            historyMsg.put("content", history.getContent());
            messages.add(historyMsg);
        }

        // 添加用户消息（增强后）
        String enhancedMessage = enhanceWithFundContext(userMessage);
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", enhancedMessage);
        messages.add(userMsg);

        return new ChatContext(getSystemPrompt(), messages);
    }

    private String getSystemPrompt() {
        return "You are a professional fund investment advisor AI assistant. " +
               "Your responsibilities are: " +
               "1. Help users analyze fund products and provide professional investment advice. " +
               "2. Answer user questions about fund investment. " +
               "3. Explain fund-related professional terms and indicators. " +
               "4. Provide market analysis and investment strategy suggestions. " +
               "5. Remind users of investment risks and do not give specific buy/sell instructions. " +
               "Response requirements: Professional, objective, and well-founded. " +
               "Use simple language to explain complex concepts. " +
               "For advice involving funds, you must indicate risks. " +
               "If users ask about specific funds, try to use the provided fund data. " +
               "Please respond in Chinese.";
    }

    private String enhanceWithFundContext(String userMessage) {
        String fundCode = extractFundCode(userMessage);
        if (fundCode != null) {
            Fund fund = fundService.getByFundCode(fundCode);
            if (fund != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("[Fund Data Context]\n");
                sb.append("Fund Code: ").append(fund.getFundCode()).append("\n");
                sb.append("Fund Name: ").append(fund.getFundName()).append("\n");
                sb.append("Fund Type: ").append(fund.getFundType()).append("\n");
                sb.append("Latest NAV: ").append(fund.getNav()).append("\n");
                sb.append("Day Growth: ").append(fund.getDayGrowth()).append("%\n");
                sb.append("Week Growth: ").append(fund.getWeekGrowth()).append("%\n");
                sb.append("Month Growth: ").append(fund.getMonthGrowth()).append("%\n");
                sb.append("Year Growth: ").append(fund.getYearGrowth()).append("%\n");
                sb.append("Total Growth: ").append(fund.getTotalGrowth()).append("%\n");
                sb.append("Fund Scale: ").append(fund.getFundScale()).append(" Billion\n");
                sb.append("Risk Level: ").append(fund.getRiskLevel()).append("\n");
                sb.append("[/Fund Data Context]\n\n");
                sb.append("User Question: ").append(userMessage);
                return sb.toString();
            }
        }
        return userMessage;
    }

    private String extractFundCode(String message) {
        Pattern pattern = Pattern.compile("\\b(\\d{6})\\b");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        return text.length() / 3;
    }

    private void updateSession(String sessionId, int tokensUsed) {
        AIChatSession session = sessionMapper.selectBySessionId(sessionId);
        if (session != null) {
            session.setTotalTokens(session.getTotalTokens() + tokensUsed);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    private void updateSessionTitle(String sessionId, String userMessage) {
        AIChatSession session = sessionMapper.selectBySessionId(sessionId);
        if (session != null && (session.getTitle() == null || session.getTitle().isEmpty())) {
            String title = userMessage.length() > 20 ? userMessage.substring(0, 20) + "..." : userMessage;
            session.setTitle(title);
            sessionMapper.updateById(session);
        }
    }

    private AIChatSessionVO convertToSessionVO(AIChatSession session) {
        AIChatSessionVO vo = new AIChatSessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    private AIChatHistoryVO convertToHistoryVO(AIChatHistory history) {
        AIChatHistoryVO vo = new AIChatHistoryVO();
        BeanUtils.copyProperties(history, vo);
        return vo;
    }

    /**
     * 聊天上下文（包含 System Prompt 和 Messages）
     */
    @Data
    @AllArgsConstructor
    private static class ChatContext {
        private String systemPrompt;
        private List<Map<String, String>> messages;
    }
}
