package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
    
    @Select("SELECT uf.id, uf.user_id as userId, uf.fund_code as fundCode, uf.group_name as groupName, uf.create_time as createTime, " +
            "f.fund_name as fundName, f.nav, f.nav_date as navDate, f.day_growth as dayGrowth, f.week_growth as weekGrowth, " +
            "f.month_growth as monthGrowth, f.three_month_growth as threeMonthGrowth, f.year_growth as yearGrowth " +
            "FROM t_user_favorite uf " +
            "LEFT JOIN t_fund f ON uf.fund_code = f.fund_code " +
            "WHERE uf.user_id = #{userId} ORDER BY uf.create_time DESC")
    List<UserFavorite> getUserFavoritesWithFundInfo(@Param("userId") Long userId);
}
