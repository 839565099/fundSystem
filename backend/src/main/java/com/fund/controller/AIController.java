package com.fund.controller;

import com.fund.common.Result;
import com.fund.dto.AIChatRequest;
import com.fund.service.AIChatService;
import com.fund.vo.AIChatHistoryVO;
import com.fund.vo.AIChatResponse;
import com.fund.vo.AIChatSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 助手控制器
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIChatService aiChatService;

    @PostMapping("/chat")
    public Result<AIChatResponse> chat(@RequestAttribute(required = false) Long userId, @RequestBody AIChatRequest request) {
        // 如果未登录，使用临时用户ID
        Long effectiveUserId = userId != null ? userId : 0L;
        AIChatResponse response = aiChatService.chat(effectiveUserId, request);
        return Result.success(response);
    }

    @GetMapping("/sessions")
    public Result<List<AIChatSessionVO>> getSessions(@RequestAttribute Long userId) {
        List<AIChatSessionVO> sessions = aiChatService.getSessions(userId);
        return Result.success(sessions);
    }

    @PostMapping("/session")
    public Result<AIChatSessionVO> createSession(@RequestAttribute Long userId) {
        AIChatSessionVO session = aiChatService.createSession(userId);
        return Result.success(session);
    }

    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@RequestAttribute Long userId, @PathVariable String sessionId) {
        aiChatService.deleteSession(userId, sessionId);
        return Result.success();
    }

    @GetMapping("/history/{sessionId}")
    public Result<List<AIChatHistoryVO>> getHistory(@RequestAttribute Long userId, @PathVariable String sessionId) {
        List<AIChatHistoryVO> history = aiChatService.getHistory(userId, sessionId);
        return Result.success(history);
    }
}
