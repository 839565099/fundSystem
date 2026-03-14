package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI对话会话实体
 */
@Data
@TableName("t_ai_chat_session")
public class AIChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话唯一标识
     */
    private String sessionId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 使用的模型类型
     */
    private String modelType;

    /**
     * 总消耗token数
     */
    private Integer totalTokens;

    /**
     * 状态: 0-已关闭 1-进行中
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 模型类型常量 ==========
    public static final String MODEL_QWEN_TURBO = "qwen-turbo";
    public static final String MODEL_QWEN_PLUS = "qwen-plus";
    public static final String MODEL_QWEN_MAX = "qwen-max";
}
