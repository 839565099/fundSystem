package com.fund.service;

import com.fund.dto.PortfolioDTO;
import com.fund.dto.PortfolioItemDTO;
import com.fund.vo.PortfolioItemVO;
import com.fund.vo.PortfolioVO;

import java.util.List;

/**
 * 投资组合服务接口
 */
public interface PortfolioService {

    /**
     * 创建投资组合
     */
    PortfolioVO createPortfolio(Long userId, PortfolioDTO dto);

    /**
     * 更新投资组合
     */
    PortfolioVO updatePortfolio(Long userId, Long portfolioId, PortfolioDTO dto);

    /**
     * 删除投资组合
     */
    void deletePortfolio(Long userId, Long portfolioId);

    /**
     * 获取用户的投资组合列表
     */
    List<PortfolioVO> getUserPortfolios(Long userId);

    /**
     * 获取投资组合详情
     */
    PortfolioVO getPortfolioDetail(Long userId, Long portfolioId);

    /**
     * 添加持仓
     */
    PortfolioItemVO addItem(Long userId, Long portfolioId, PortfolioItemDTO dto);

    /**
     * 更新持仓
     */
    PortfolioItemVO updateItem(Long userId, Long portfolioId, Long itemId, PortfolioItemDTO dto);

    /**
     * 删除持仓
     */
    void deleteItem(Long userId, Long portfolioId, Long itemId);

    /**
     * 刷新组合数据（更新净值、计算收益）
     */
    void refreshPortfolio(Long portfolioId);

    /**
     * 获取用户的默认组合
     */
    PortfolioVO getDefaultPortfolio(Long userId);

    /**
     * 设置默认组合
     */
    void setDefaultPortfolio(Long userId, Long portfolioId);
}
