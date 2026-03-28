package com.fund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统日志实体
 */
@Data
@TableName("t_system_log")
public class SystemLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 日志类型: DATA_SYNC, DATA_CLEANUP, SCHEDULED_TASK 等
     */
    private String logType;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 状态: SUCCESS, FAILED
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 执行耗时(毫秒)
     */
    private Integer durationMs;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
