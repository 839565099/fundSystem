package com.fund.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private String role; // 角色: ADMIN-管理员, USER-普通用户
    private Integer status;
    private LocalDateTime createTime;
}
