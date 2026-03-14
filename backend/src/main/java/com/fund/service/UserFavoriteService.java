package com.fund.service;

import com.fund.entity.UserFavorite;
import java.util.List;

public interface UserFavoriteService {
    
    List<UserFavorite> getUserFavorites(Long userId);
    
    List<UserFavorite> getUserFavoritesWithFundInfo(Long userId);
    
    void addFavorite(Long userId, String fundCode);
    
    void removeFavorite(Long userId, String fundCode);
    
    boolean isFavorite(Long userId, String fundCode);
}
