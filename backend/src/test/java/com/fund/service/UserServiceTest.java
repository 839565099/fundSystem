package com.fund.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.common.ErrorCode;
import com.fund.dto.LoginDTO;
import com.fund.dto.RegisterDTO;
import com.fund.entity.User;
import com.fund.exception.BusinessException;
import com.fund.mapper.UserMapper;
import com.fund.service.impl.UserServiceImpl;
import com.fund.util.JwtUtil;
import com.fund.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private RegisterDTO registerDTO;
    private LoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        // 准备测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(BCrypt.hashpw("password123"));
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setNickname("测试用户");
        testUser.setStatus(1);

        // 准备注册 DTO
        registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("new@example.com");
        registerDTO.setPhone("13900139000");
        registerDTO.setNickname("新用户");

        // 准备登录 DTO
        loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");
    }

    @Test
    @DisplayName("注册成功")
    void register_success() {
        // given - 用户名、邮箱、手机号都不存在
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });

        // when
        UserVO result = userService.register(registerDTO);

        // then
        assertNotNull(result);
        assertEquals(registerDTO.getUsername(), result.getUsername());
        assertEquals(registerDTO.getEmail(), result.getEmail());
        verify(userMapper, times(3)).selectCount(any(LambdaQueryWrapper.class));
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_usernameExists() {
        // given
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.register(registerDTO));

        assertEquals(ErrorCode.USERNAME_EXISTS.getCode(), exception.getCode());
        verify(userMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("注册失败 - 邮箱已存在")
    void register_emailExists() {
        // given
        when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(0L)  // 用户名不存在
            .thenReturn(1L); // 邮箱存在

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.register(registerDTO));

        assertEquals(ErrorCode.EMAIL_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录成功")
    void login_success() {
        // given
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(jwtUtil.generateToken(testUser.getId(), testUser.getUsername())).thenReturn("test-token");

        // when
        String token = userService.login(loginDTO);

        // then
        assertNotNull(token);
        assertEquals("test-token", token);
        verify(userMapper, times(1)).updateById(any(User.class));
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void login_userNotFound() {
        // given
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.login(loginDTO));

        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_passwordError() {
        // given
        loginDTO.setPassword("wrongpassword");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.login(loginDTO));

        assertEquals(ErrorCode.PASSWORD_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录失败 - 用户被禁用")
    void login_userDisabled() {
        // given
        testUser.setStatus(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.login(loginDTO));

        assertEquals(ErrorCode.USER_DISABLED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("获取用户信息成功")
    void getUserInfo_success() {
        // given
        when(userMapper.selectById(1L)).thenReturn(testUser);

        // when
        UserVO result = userService.getUserInfo(1L);

        // then
        assertNotNull(result);
        assertEquals(testUser.getUsername(), result.getUsername());
    }

    @Test
    @DisplayName("获取用户信息失败 - 用户不存在")
    void getUserInfo_notFound() {
        // given
        when(userMapper.selectById(999L)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.getUserInfo(999L));

        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("修改密码成功")
    void updatePassword_success() {
        // given
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // when
        assertDoesNotThrow(() -> userService.updatePassword(1L, "password123", "newpassword"));

        // then
        verify(userMapper, times(1)).updateById(any(User.class));
    }

    @Test
    @DisplayName("修改密码失败 - 旧密码错误")
    void updatePassword_wrongOldPassword() {
        // given
        when(userMapper.selectById(1L)).thenReturn(testUser);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.updatePassword(1L, "wrongpassword", "newpassword"));

        assertEquals(ErrorCode.PASSWORD_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("根据用户名查询用户")
    void getByUsername_success() {
        // given
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        // when
        User result = userService.getByUsername("testuser");

        // then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }
}
