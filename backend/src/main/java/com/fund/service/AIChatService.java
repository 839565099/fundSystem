package com.fund.service;

import com.fund.dto.AIChatRequest;
import com.fund.vo.AIChatResponse;
import com.fund.vo.AIChatSessionVO;
import com.fund.vo.AIChatHistoryVO;

import java.util.List;

/**
 * AI 聊天服务接口
 */
public interface AIChatService {

    /**
     * 发送消息并获取AI回复
     */
    AIChatResponse chat(Long userId, AIChatRequest request);

    /**
     * 获取用户的会话列表
     */
    List<AIChatSessionVO> getSessions(Long userId);

    /**
     * 创建新会话
     */
    AIChatSessionVO createSession(Long userId);

    /**
     * 删除会话
     */
    void deleteSession(Long userId, String sessionId);

    /**
     * 获取会话历史
     */
    List<AIChatHistoryVO> getHistory(Long userId, String sessionId);
}
