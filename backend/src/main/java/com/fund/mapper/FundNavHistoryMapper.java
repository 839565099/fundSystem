package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.FundNavHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FundNavHistoryMapper extends BaseMapper<FundNavHistory> {

    @Select("SELECT * FROM t_fund_nav_history WHERE fund_code = #{fundCode} " +
            "AND nav_date >= #{startDate} ORDER BY nav_date ASC")
    List<FundNavHistory> getHistoryByDateRange(@Param("fundCode") String fundCode,
                                                @Param("startDate") LocalDate startDate);

    @Select("SELECT * FROM t_fund_nav_history WHERE fund_code = #{fundCode} " +
            "ORDER BY nav_date DESC LIMIT #{limit}")
    List<FundNavHistory> getRecentHistory(@Param("fundCode") String fundCode, @Param("limit") int limit);

    @Select("SELECT * FROM t_fund_nav_history WHERE fund_code = #{fundCode} " +
            "AND nav_date BETWEEN #{startDate} AND #{endDate} ORDER BY nav_date ASC")
    List<FundNavHistory> findByFundCodeAndNavDateBetween(@Param("fundCode") String fundCode,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);
}
