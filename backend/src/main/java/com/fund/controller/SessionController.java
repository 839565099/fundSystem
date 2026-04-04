package com.fund.controller;

import com.fund.annotation.RequireAdmin;
import com.fund.common.PageResult;
import com.fund.common.Result;
import com.fund.dto.SessionConfigUpdateDTO;
import com.fund.entity.SessionConfig;
import com.fund.service.SessionService;
import com.fund.vo.SessionInfoVO;
import com.fund.vo.SessionLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/sessions")
@RequireAdmin
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public Result<List<SessionInfoVO>> getActiveSessions() {
        return Result.success(sessionService.getActiveSessions());
    }

    @DeleteMapping("/{userId}")
    public Result<?> kickUser(@PathVariable Long userId) {
        sessionService.kickUser(userId);
        return Result.success("用户已强制下线");
    }

    @GetMapping("/config")
    public Result<List<SessionConfig>> getSessionConfigs() {
        return Result.success(sessionService.getSessionConfigs());
    }

    @PutMapping("/config/{roleName}")
    public Result<?> updateSessionConfig(@PathVariable String roleName,
                                         @Valid @RequestBody SessionConfigUpdateDTO dto) {
        sessionService.updateSessionConfig(roleName, dto);
        return Result.success("配置已更新");
    }

    @GetMapping("/logs")
    public Result<PageResult<SessionLogVO>> getSessionLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(sessionService.getSessionLogs(page, pageSize, eventType, username, startTime, endTime));
    }
}
