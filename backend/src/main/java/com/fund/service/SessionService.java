package com.fund.service;

import com.fund.common.PageResult;
import com.fund.dto.SessionConfigUpdateDTO;
import com.fund.entity.SessionConfig;
import com.fund.vo.LoginVO;
import com.fund.vo.SessionInfoVO;
import com.fund.vo.SessionLogVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface SessionService {

    LoginVO.SessionInfo createSession(Long userId, String username, String role, HttpServletRequest request);

    void destroySession(Long userId);

    void kickUser(Long userId);

    boolean validateSession(Long userId);

    List<SessionInfoVO> getActiveSessions();

    List<SessionConfig> getSessionConfigs();

    void updateSessionConfig(String roleName, SessionConfigUpdateDTO dto);

    PageResult<SessionLogVO> getSessionLogs(int page, int pageSize, String eventType, String username, String startTime, String endTime);
}
