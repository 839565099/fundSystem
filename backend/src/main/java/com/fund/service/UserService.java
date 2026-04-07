package com.fund.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.dto.LoginDTO;
import com.fund.dto.RegisterDTO;
import com.fund.entity.User;
import com.fund.vo.LoginVO;
import com.fund.vo.UserVO;
import java.util.Map;

public interface UserService {

    UserVO register(RegisterDTO registerDTO);

    LoginVO login(LoginDTO loginDTO);
    
    UserVO getUserInfo(Long userId);
    
    UserVO updateUserInfo(Long userId, User user);
    
    void updatePassword(Long userId, String oldPassword, String newPassword);
    
    User getByUsername(String username);

    Map<String, Object> getUserStats(Long userId);

    User findByEmail(String email);

    User findByGoogleId(String googleId);

    LoginVO loginWithGoogle(String googleId, String email, String name, String avatar, javax.servlet.http.HttpServletRequest request);

    UserVO updateUsername(Long userId, String newUsername);

    void setPassword(Long userId, String password);

    boolean hasPassword(Long userId);
}
