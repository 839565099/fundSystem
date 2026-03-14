package com.fund.controller;

import com.fund.common.Result;
import com.fund.dto.PortfolioDTO;
import com.fund.dto.PortfolioItemDTO;
import com.fund.service.PortfolioService;
import com.fund.vo.PortfolioItemVO;
import com.fund.vo.PortfolioVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 投资组合控制器
 */
@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping
    public Result<PortfolioVO> createPortfolio(@RequestAttribute Long userId, @RequestBody PortfolioDTO dto) {
        PortfolioVO vo = portfolioService.createPortfolio(userId, dto);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    public Result<PortfolioVO> updatePortfolio(@RequestAttribute Long userId,
                                               @PathVariable Long id,
                                               @RequestBody PortfolioDTO dto) {
        PortfolioVO vo = portfolioService.updatePortfolio(userId, id, dto);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePortfolio(@RequestAttribute Long userId, @PathVariable Long id) {
        portfolioService.deletePortfolio(userId, id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<PortfolioVO>> getUserPortfolios(@RequestAttribute Long userId) {
        List<PortfolioVO> portfolios = portfolioService.getUserPortfolios(userId);
        return Result.success(portfolios);
    }

    @GetMapping("/{id}")
    public Result<PortfolioVO> getPortfolioDetail(@RequestAttribute Long userId, @PathVariable Long id) {
        PortfolioVO vo = portfolioService.getPortfolioDetail(userId, id);
        return Result.success(vo);
    }

    @PostMapping("/{id}/item")
    public Result<PortfolioItemVO> addItem(@RequestAttribute Long userId,
                                           @PathVariable Long id,
                                           @RequestBody PortfolioItemDTO dto) {
        PortfolioItemVO vo = portfolioService.addItem(userId, id, dto);
        return Result.success(vo);
    }

    @PutMapping("/{portfolioId}/item/{itemId}")
    public Result<PortfolioItemVO> updateItem(@RequestAttribute Long userId,
                                              @PathVariable Long portfolioId,
                                              @PathVariable Long itemId,
                                              @RequestBody PortfolioItemDTO dto) {
        PortfolioItemVO vo = portfolioService.updateItem(userId, portfolioId, itemId, dto);
        return Result.success(vo);
    }

    @DeleteMapping("/{portfolioId}/item/{itemId}")
    public Result<Void> deleteItem(@RequestAttribute Long userId,
                                   @PathVariable Long portfolioId,
                                   @PathVariable Long itemId) {
        portfolioService.deleteItem(userId, portfolioId, itemId);
        return Result.success();
    }

    @PostMapping("/{id}/refresh")
    public Result<Void> refreshPortfolio(@RequestAttribute Long userId, @PathVariable Long id) {
        portfolioService.refreshPortfolio(id);
        return Result.success();
    }

    @GetMapping("/default")
    public Result<PortfolioVO> getDefaultPortfolio(@RequestAttribute Long userId) {
        PortfolioVO vo = portfolioService.getDefaultPortfolio(userId);
        return Result.success(vo);
    }

    @PostMapping("/{id}/set-default")
    public Result<Void> setDefaultPortfolio(@RequestAttribute Long userId, @PathVariable Long id) {
        portfolioService.setDefaultPortfolio(userId, id);
        return Result.success();
    }
}
