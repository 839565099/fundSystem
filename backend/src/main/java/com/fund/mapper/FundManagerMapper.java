package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.FundManager;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FundManagerMapper extends BaseMapper<FundManager> {
    
    @Select("SELECT fm.* FROM t_fund_manager fm " +
            "JOIN t_fund_manager_relation fmr ON fm.manager_id = fmr.manager_id " +
            "WHERE fmr.fund_code = #{fundCode}")
    List<FundManager> getManagersByFundCode(@Param("fundCode") String fundCode);
}
