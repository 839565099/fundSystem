package com.fund.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 板块历史数据VO
 */
@Data
public class SectorHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 板块代码 */
    private String code;

    /** 板块名称 */
    private String name;

    /** 历史K线数据 */
    private List<KLineItem> history;

    @Data
    public static class KLineItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 日期 */
        private String date;
        /** 开盘价 */
        private BigDecimal open;
        /** 收盘价 */
        private BigDecimal close;
        /** 最高价 */
        private BigDecimal high;
        /** 最低价 */
        private BigDecimal low;
        /** 成交额(亿) */
        private BigDecimal volume;
        /** 涨跌幅 */
        private BigDecimal changePercent;
    }
}
