package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.common.ErrorCode;
import com.fund.entity.UserFavorite;
import com.fund.exception.BusinessException;
import com.fund.mapper.UserFavoriteMapper;
import com.fund.service.FundService;
import com.fund.service.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFavoriteServiceImpl implements UserFavoriteService {
    
    private final UserFavoriteMapper userFavoriteMapper;
    private final FundService fundService;
    
    @Override
    public List<UserFavorite> getUserFavorites(Long userId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId);
        return userFavoriteMapper.selectList(wrapper);
    }
    
    @Override
    public List<UserFavorite> getUserFavoritesWithFundInfo(Long userId) {
        return userFavoriteMapper.getUserFavoritesWithFundInfo(userId);
    }
    
    @Override
    public void addFavorite(Long userId, String fundCode) {
        if (isFavorite(userId, fundCode)) {
            throw new BusinessException(ErrorCode.FAVORITE_EXISTS);
        }

        long count = userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>().eq(UserFavorite::getUserId, userId));
        if (count >= 50) {
            throw new BusinessException(ErrorCode.FAVORITE_LIMIT);
        }

        // 校验基金是否存在
        try {
            com.fund.vo.FundDetailVO fundDetail = fundService.getFundDetail(fundCode);
            if (fundDetail == null) {
                throw new BusinessException(ErrorCode.FUND_NOT_FOUND);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取基金信息失败，基金代码无效: {}", fundCode, e);
            throw new BusinessException(ErrorCode.FUND_NOT_FOUND);
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setFundCode(fundCode);
        favorite.setGroupName("默认分组");
        userFavoriteMapper.insert(favorite);
    }
    
    @Override
    public void removeFavorite(Long userId, String fundCode) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getFundCode, fundCode);
        userFavoriteMapper.delete(wrapper);
    }
    
    @Override
    public boolean isFavorite(Long userId, String fundCode) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getFundCode, fundCode);
        return userFavoriteMapper.selectCount(wrapper) > 0;
    }
}
