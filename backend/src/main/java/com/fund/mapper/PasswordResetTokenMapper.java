package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.PasswordResetToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 密码重置令牌 Mapper
 */
@Mapper
public interface PasswordResetTokenMapper extends BaseMapper<PasswordResetToken> {

    /**
     * 标记令牌已使用
     */
    @Update("UPDATE t_password_reset_token SET used_time = NOW() WHERE token = #{token}")
    int markAsUsed(@Param("token") String token);

    /**
     * 使邮箱所有未使用的令牌失效
     */
    @Update("UPDATE t_password_reset_token SET used_time = NOW() WHERE email = #{email} AND used_time IS NULL")
    int invalidateByEmail(@Param("email") String email);
}
