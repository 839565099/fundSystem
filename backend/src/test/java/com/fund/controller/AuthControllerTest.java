package com.fund.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.common.ErrorCode;
import com.fund.dto.ChangePasswordDTO;
import com.fund.dto.LoginDTO;
import com.fund.dto.RegisterDTO;
import com.fund.dto.UpdateProfileDTO;
import com.fund.exception.BusinessException;
import com.fund.service.UserService;
import com.fund.vo.LoginVO;
import com.fund.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证控制器测试
 * 使用 @SpringBootTest 配合 @AutoConfigureMockMvc 进行集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private RegisterDTO registerDTO;
    private LoginDTO loginDTO;
    private UserVO userVO;

    @BeforeEach
    void setUp() {
        // 准备注册 DTO
        registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("test@example.com");
        registerDTO.setPhone("13800138000");

        // 准备登录 DTO
        loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        // 准备用户 VO
        userVO = new UserVO();
        userVO.setId(1L);
        userVO.setUsername("testuser");
        userVO.setEmail("test@example.com");
        userVO.setPhone("13800138000");
        userVO.setNickname("测试用户");
    }

    @Test
    @DisplayName("注册成功")
    void register_success() throws Exception {
        // given
        when(userService.register(any(RegisterDTO.class))).thenReturn(userVO);

        // when & then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("注册成功"))
            .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService, times(1)).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_usernameExists() throws Exception {
        // given
        when(userService.register(any(RegisterDTO.class)))
            .thenThrow(new BusinessException(ErrorCode.USERNAME_EXISTS));

        // when & then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_EXISTS.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.USERNAME_EXISTS.getMessage()));
    }

    @Test
    @DisplayName("登录成功")
    void login_success() throws Exception {
        // given
        LoginVO loginVO = new LoginVO();
        loginVO.setToken("test-jwt-token");
        loginVO.setUser(userVO);
        when(userService.login(any(LoginDTO.class))).thenReturn(loginVO);

        // when & then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("登录成功"))
            .andExpect(jsonPath("$.data.token").value("test-jwt-token"));

        verify(userService, times(1)).login(any(LoginDTO.class));
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void login_userNotFound() throws Exception {
        // given
        when(userService.login(any(LoginDTO.class)))
            .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_passwordError() throws Exception {
        // given
        when(userService.login(any(LoginDTO.class)))
            .thenThrow(new BusinessException(ErrorCode.PASSWORD_ERROR));

        // when & then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_ERROR.getCode()));
    }

    @Test
    @DisplayName("更新用户资料成功")
    void updateProfile_success() throws Exception {
        // given
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("新昵称");
        dto.setEmail("new@example.com");

        UserVO updatedVO = new UserVO();
        updatedVO.setId(1L);
        updatedVO.setUsername("testuser");
        updatedVO.setNickname("新昵称");
        updatedVO.setEmail("new@example.com");

        when(userService.updateUserInfo(any(Long.class), any())).thenReturn(updatedVO);

        // when & then
        mockMvc.perform(put("/auth/profile")
                .requestAttr("userId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("更新成功"));

        verify(userService, times(1)).updateUserInfo(any(Long.class), any());
    }

    @Test
    @DisplayName("修改密码成功")
    void changePassword_success() throws Exception {
        // given
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("oldpassword");
        dto.setNewPassword("newpassword123");

        doNothing().when(userService).updatePassword(1L, "oldpassword", "newpassword123");

        // when & then
        mockMvc.perform(put("/auth/password")
                .requestAttr("userId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("密码修改成功"));

        verify(userService, times(1)).updatePassword(1L, "oldpassword", "newpassword123");
    }

    @Test
    @DisplayName("修改密码失败 - 旧密码错误")
    void changePassword_wrongOldPassword() throws Exception {
        // given
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("wrongpassword");
        dto.setNewPassword("newpassword123");

        doThrow(new BusinessException(ErrorCode.PASSWORD_ERROR))
            .when(userService).updatePassword(1L, "wrongpassword", "newpassword123");

        // when & then
        mockMvc.perform(put("/auth/password")
                .requestAttr("userId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_ERROR.getCode()));
    }
}
