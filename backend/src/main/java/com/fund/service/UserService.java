package com.fund.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.dto.LoginDTO;
import com.fund.dto.RegisterDTO;
import com.fund.entity.User;
import com.fund.vo.UserVO;

public interface UserService {
    
    UserVO register(RegisterDTO registerDTO);
    
    String login(LoginDTO loginDTO);
    
    UserVO getUserInfo(Long userId);
    
    UserVO updateUserInfo(Long userId, User user);
    
    void updatePassword(Long userId, String oldPassword, String newPassword);
    
    User getByUsername(String username);
}
