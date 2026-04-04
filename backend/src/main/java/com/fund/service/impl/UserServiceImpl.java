package com.fund.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.common.ErrorCode;
import com.fund.dto.LoginDTO;
import com.fund.dto.RegisterDTO;
import com.fund.entity.User;
import com.fund.exception.BusinessException;
import com.fund.mapper.UserMapper;
import com.fund.mapper.UserFavoriteMapper;
import com.fund.mapper.PortfolioMapper;
import com.fund.mapper.UserAlertRuleMapper;
import com.fund.entity.UserFavorite;
import com.fund.entity.Portfolio;
import com.fund.entity.UserAlertRule;
import com.fund.service.UserService;
import com.fund.service.SessionService;
import com.fund.util.JwtUtil;
import com.fund.vo.LoginVO;
import com.fund.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final PortfolioMapper portfolioMapper;
    private final UserAlertRuleMapper userAlertRuleMapper;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;
    private final HttpServletRequest httpServletRequest;

    @Override
    public UserVO register(RegisterDTO registerDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        
        if (registerDTO.getEmail() != null) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, registerDTO.getEmail());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
        }
        
        if (registerDTO.getPhone() != null) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, registerDTO.getPhone());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
        }
        
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setStatus(1);
        user.setRole("USER"); // 默认角色为普通用户

        userMapper.insert(user);
        
        return convertToVO(user);
    }
    
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String role = user.getRole() != null ? user.getRole() : "USER";
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);

        // 创建会话
        LoginVO.SessionInfo sessionInfo = sessionService.createSession(
                user.getId(), user.getUsername(), role, httpServletRequest);

        // 构建返回值
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(convertToVO(user));
        loginVO.setSessionInfo(sessionInfo);
        return loginVO;
    }
    
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }
    
    @Override
    public UserVO updateUserInfo(Long userId, User userInfo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        if (userInfo.getNickname() != null) {
            user.setNickname(userInfo.getNickname());
        }
        if (userInfo.getAvatar() != null) {
            user.setAvatar(userInfo.getAvatar());
        }
        if (userInfo.getEmail() != null) {
            user.setEmail(userInfo.getEmail());
        }
        if (userInfo.getPhone() != null) {
            user.setPhone(userInfo.getPhone());
        }
        
        userMapper.updateById(user);
        return convertToVO(user);
    }
    
    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        
        user.setPassword(BCrypt.hashpw(newPassword));
        userMapper.updateById(user);
    }
    
    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public Map<String, Object> getUserStats(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        LambdaQueryWrapper<UserFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(UserFavorite::getUserId, userId);
        long favoriteCount = userFavoriteMapper.selectCount(favWrapper);

        LambdaQueryWrapper<Portfolio> portWrapper = new LambdaQueryWrapper<>();
        portWrapper.eq(Portfolio::getUserId, userId);
        long portfolioCount = portfolioMapper.selectCount(portWrapper);

        LambdaQueryWrapper<UserAlertRule> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.eq(UserAlertRule::getUserId, userId);
        long alertCount = userAlertRuleMapper.selectCount(alertWrapper);

        long registerDays = 0;
        if (user.getCreateTime() != null) {
            registerDays = ChronoUnit.DAYS.between(user.getCreateTime().toLocalDate(), LocalDateTime.now().toLocalDate());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("favoriteCount", favoriteCount);
        stats.put("portfolioCount", portfolioCount);
        stats.put("alertCount", alertCount);
        stats.put("registerDays", registerDays);
        return stats;
    }
    
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
