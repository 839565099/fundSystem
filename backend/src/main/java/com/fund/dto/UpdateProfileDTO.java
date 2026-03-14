package com.fund.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Data
public class UpdateProfileDTO {
    
    @Size(max = 20, message = "昵称最多20个字符")
    private String nickname;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phone;
    
    private String avatar;
}
