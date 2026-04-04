package com.fund.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class SessionConfigUpdateDTO {

    @NotNull(message = "最大在线时长不能为空")
    @Min(value = 1, message = "最大在线时长不能小于1分钟")
    private Integer maxDurationMinutes;

    @NotNull(message = "提醒时间不能为空")
    @Min(value = 1, message = "提醒时间不能小于1分钟")
    private Integer warningMinutes;

    @NotNull(message = "启用状态不能为空")
    private Boolean isEnabled;
}
