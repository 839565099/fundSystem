package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.FundHoldings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FundHoldingsMapper extends BaseMapper<FundHoldings> {
    
    @Select("SELECT * FROM t_fund_holdings WHERE fund_code = #{fundCode} " +
            "ORDER BY report_date DESC, holding_ratio DESC LIMIT #{limit}")
    List<FundHoldings> getLatestHoldings(@Param("fundCode") String fundCode, @Param("limit") int limit);
}
