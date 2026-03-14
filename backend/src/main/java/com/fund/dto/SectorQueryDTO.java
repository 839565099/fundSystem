package com.fund.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 板块查询参数DTO
 */
@Data
public class SectorQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 板块类型: all-全部, industry-行业, concept-概念, region-地域 */
    private String type = "all";

    /** 排序字段: dayGrowth, weekGrowth, monthGrowth, volume, turnover */
    private String sortBy = "dayGrowth";

    /** 排序方式: asc-升序, desc-降序 */
    private String sortOrder = "desc";

    /** 搜索关键词 */
    private String keyword;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 20;
}
