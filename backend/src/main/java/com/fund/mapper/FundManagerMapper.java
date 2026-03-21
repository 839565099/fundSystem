package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.FundManager;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FundManagerMapper extends BaseMapper<FundManager> {

    @Select("SELECT fm.* FROM t_fund_manager fm " +
            "JOIN t_fund_manager_relation fmr ON fm.manager_id = fmr.manager_id " +
            "WHERE fmr.fund_code = #{fundCode}")
    List<FundManager> getManagersByFundCode(@Param("fundCode") String fundCode);

    @Insert("INSERT INTO t_fund_manager (manager_id, manager_name, company, work_years, start_date, " +
            "total_asset, best_return, education, resume, investment_idea, photo, create_time) " +
            "VALUES (#{managerId}, #{managerName}, #{company}, #{workYears}, #{startDate}, " +
            "#{totalAsset}, #{bestReturn}, #{education}, #{resume}, #{investmentIdea}, #{photo}, NOW()) " +
            "ON DUPLICATE KEY UPDATE manager_name = #{managerName}, company = #{company}, " +
            "work_years = #{workYears}, total_asset = #{totalAsset}, best_return = #{bestReturn}, " +
            "education = #{education}, resume = #{resume}, investment_idea = #{investmentIdea}, " +
            "photo = #{photo}, update_time = NOW()")
    void insertOrUpdate(FundManager manager);

    @Insert("INSERT INTO t_fund_manager_relation (fund_code, manager_id, start_date, create_time) " +
            "VALUES (#{fundCode}, #{managerId}, #{startDate}, NOW()) " +
            "ON DUPLICATE KEY UPDATE start_date = #{startDate}")
    void insertRelation(@Param("fundCode") String fundCode,
                        @Param("managerId") String managerId,
                        @Param("startDate") LocalDate startDate);
}
