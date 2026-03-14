package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.Fund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FundMapper extends BaseMapper<Fund> {
    
    @Select("SELECT * FROM t_fund WHERE fund_code LIKE CONCAT('%', #{keyword}, '%') " +
            "OR fund_name LIKE CONCAT('%', #{keyword}, '%') LIMIT #{limit}")
    List<Fund> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
    
    @Select("SELECT * FROM t_fund ORDER BY day_growth DESC LIMIT #{limit}")
    List<Fund> getTopGrowth(@Param("limit") int limit);
    
    @Select("SELECT * FROM t_fund ORDER BY fund_scale DESC LIMIT #{limit}")
    List<Fund> getHotFunds(@Param("limit") int limit);
    
    @Select("SELECT * FROM t_fund WHERE fund_code = #{fundCode}")
    Fund getByFundCode(@Param("fundCode") String fundCode);
}
