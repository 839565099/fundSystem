package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.FundHoldings;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FundHoldingsMapper extends BaseMapper<FundHoldings> {

    @Select("SELECT * FROM t_fund_holdings WHERE fund_code = #{fundCode} " +
            "ORDER BY report_date DESC, holding_ratio DESC LIMIT #{limit}")
    List<FundHoldings> getLatestHoldings(@Param("fundCode") String fundCode, @Param("limit") int limit);

    @Insert("<script>" +
            "INSERT INTO t_fund_holdings (fund_code, report_date, stock_code, stock_name, " +
            "holding_ratio, holding_shares, holding_value, holding_type, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.fundCode}, #{item.reportDate}, #{item.stockCode}, #{item.stockName}, " +
            "#{item.holdingRatio}, #{item.holdingShares}, #{item.holdingValue}, #{item.holdingType}, NOW())" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<FundHoldings> holdings);
}
