package com.fund.controller;

import com.fund.common.Result;
import com.fund.entity.UserFavorite;
import com.fund.service.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {
    
    private final UserFavoriteService userFavoriteService;
    
    @GetMapping("/list")
    public Result<List<UserFavorite>> getFavorites(@RequestAttribute Long userId) {
        List<UserFavorite> favorites = userFavoriteService.getUserFavoritesWithFundInfo(userId);
        return Result.success(favorites);
    }
    
    @PostMapping("/add")
    public Result<String> addFavorite(@RequestAttribute Long userId,
                                     @RequestParam String fundCode) {
        userFavoriteService.addFavorite(userId, fundCode);
        return Result.success("收藏成功");
    }
    
    @DeleteMapping("/remove/{fundCode}")
    public Result<String> removeFavorite(@RequestAttribute Long userId,
                                        @PathVariable String fundCode) {
        userFavoriteService.removeFavorite(userId, fundCode);
        return Result.success("取消收藏成功");
    }
    
    @GetMapping("/check")
    public Result<Boolean> isFavorite(@RequestAttribute Long userId,
                                       @RequestParam String fundCode) {
        boolean isFavorite = userFavoriteService.isFavorite(userId, fundCode);
        return Result.success(isFavorite);
    }
}
