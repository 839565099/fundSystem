package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.dto.PortfolioDTO;
import com.fund.dto.PortfolioItemDTO;
import com.fund.entity.Fund;
import com.fund.entity.Portfolio;
import com.fund.entity.PortfolioItem;
import com.fund.exception.BusinessException;
import com.fund.mapper.PortfolioItemMapper;
import com.fund.mapper.PortfolioMapper;
import com.fund.service.FundService;
import com.fund.service.PortfolioService;
import com.fund.vo.AssetAllocationVO;
import com.fund.vo.PortfolioItemVO;
import com.fund.vo.PortfolioVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 投资组合服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioMapper portfolioMapper;
    private final PortfolioItemMapper portfolioItemMapper;
    private final FundService fundService;

    // 资产配置颜色
    private static final String[] COLORS = {
            "#3b82f6", "#22c55e", "#f59e0b", "#ef4444", "#8b5cf6",
            "#ec4899", "#06b6d4", "#84cc16", "#f97316", "#6366f1"
    };

    @Override
    @Transactional
    public PortfolioVO createPortfolio(Long userId, PortfolioDTO dto) {
        Portfolio portfolio = new Portfolio();
        portfolio.setUserId(userId);
        portfolio.setName(dto.getName());
        portfolio.setDescription(dto.getDescription());
        portfolio.setTotalAmount(BigDecimal.ZERO);
        portfolio.setCurrentValue(BigDecimal.ZERO);
        portfolio.setTotalProfit(BigDecimal.ZERO);
        portfolio.setTotalReturn(BigDecimal.ZERO);
        portfolio.setDayProfit(BigDecimal.ZERO);
        portfolio.setDayReturn(BigDecimal.ZERO);
        portfolio.setFundCount(0);
        portfolio.setStatus(1);
        portfolio.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : 0);

        portfolioMapper.insert(portfolio);

        // 如果设置为默认，取消其他默认
        if (portfolio.getIsDefault() == 1) {
            clearOtherDefault(userId, portfolio.getId());
        }

        return convertToVO(portfolio);
    }

    @Override
    @Transactional
    public PortfolioVO updatePortfolio(Long userId, Long portfolioId, PortfolioDTO dto) {
        Portfolio portfolio = getAndCheckOwner(userId, portfolioId);

        if (dto.getName() != null) {
            portfolio.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            portfolio.setDescription(dto.getDescription());
        }
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            portfolio.setIsDefault(1);
            clearOtherDefault(userId, portfolioId);
        }

        portfolioMapper.updateById(portfolio);
        return convertToVO(portfolio);
    }

    @Override
    @Transactional
    public void deletePortfolio(Long userId, Long portfolioId) {
        Portfolio portfolio = getAndCheckOwner(userId, portfolioId);

        // 删除所有持仓
        LambdaQueryWrapper<PortfolioItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PortfolioItem::getPortfolioId, portfolioId);
        portfolioItemMapper.delete(itemWrapper);

        // 删除组合
        portfolioMapper.deleteById(portfolioId);
    }

    @Override
    public List<PortfolioVO> getUserPortfolios(Long userId) {
        LambdaQueryWrapper<Portfolio> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Portfolio::getUserId, userId)
                .eq(Portfolio::getStatus, 1)
                .orderByDesc(Portfolio::getIsDefault)
                .orderByDesc(Portfolio::getUpdateTime);

        List<Portfolio> portfolios = portfolioMapper.selectList(wrapper);
        return portfolios.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public PortfolioVO getPortfolioDetail(Long userId, Long portfolioId) {
        Portfolio portfolio = getAndCheckOwner(userId, portfolioId);
        // 先刷新数据，确保使用最新净值计算收益
        refreshPortfolio(portfolioId);
        // 重新获取刷新后的数据
        portfolio = portfolioMapper.selectById(portfolioId);
        return convertToVOWithItems(portfolio);
    }

    @Override
    @Transactional
    public PortfolioItemVO addItem(Long userId, Long portfolioId, PortfolioItemDTO dto) {
        Portfolio portfolio = getAndCheckOwner(userId, portfolioId);

        // 检查基金是否存在，如果不存在则自动获取
        Fund fund = fundService.getByFundCode(dto.getFundCode());
        if (fund == null) {
            // 尝试从外部 API 获取基金信息
            fund = fundService.fetchAndSaveFund(dto.getFundCode());
            if (fund == null) {
                throw new BusinessException("基金代码不存在，请检查后重试");
            }
        }

        // 检查基金净值是否存在
        if (fund.getNav() == null) {
            throw new BusinessException("基金净值数据不完整，请稍后重试或选择其他基金");
        }

        // 检查是否已存在
        LambdaQueryWrapper<PortfolioItem> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(PortfolioItem::getPortfolioId, portfolioId)
                .eq(PortfolioItem::getFundCode, dto.getFundCode());
        if (portfolioItemMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException("该基金已在组合中");
        }

        PortfolioItem item = new PortfolioItem();
        item.setPortfolioId(portfolioId);
        item.setFundCode(dto.getFundCode());
        item.setFundName(fund.getFundName());
        item.setAmount(dto.getAmount());
        item.setShares(dto.getShares());
        item.setTargetRatio(dto.getTargetRatio());
        item.setBuyNav(dto.getBuyNav() != null ? dto.getBuyNav() : fund.getNav());
        item.setBuyDate(dto.getBuyDate());
        item.setCurrentNav(fund.getNav());
        item.setStatus(1);

        // 如果没有输入份额，根据金额和买入净值计算份额
        if ((item.getShares() == null || item.getShares().compareTo(BigDecimal.ZERO) == 0)
                && item.getAmount() != null && item.getBuyNav() != null
                && item.getBuyNav().compareTo(BigDecimal.ZERO) > 0) {
            item.setShares(item.getAmount().divide(item.getBuyNav(), 2, RoundingMode.HALF_UP));
        }

        // 计算当前市值和盈亏
        calculateItemValue(item, fund);

        portfolioItemMapper.insert(item);

        // 刷新组合数据
        refreshPortfolio(portfolioId);

        return convertItemToVO(item);
    }

    @Override
    @Transactional
    public PortfolioItemVO updateItem(Long userId, Long portfolioId, Long itemId, PortfolioItemDTO dto) {
        getAndCheckOwner(userId, portfolioId);

        PortfolioItem item = portfolioItemMapper.selectById(itemId);
        if (item == null || !item.getPortfolioId().equals(portfolioId)) {
            throw new BusinessException("持仓不存在");
        }

        if (dto.getAmount() != null) {
            item.setAmount(dto.getAmount());
        }
        if (dto.getShares() != null) {
            item.setShares(dto.getShares());
        }
        if (dto.getTargetRatio() != null) {
            item.setTargetRatio(dto.getTargetRatio());
        }
        if (dto.getBuyNav() != null) {
            item.setBuyNav(dto.getBuyNav());
        }
        if (dto.getBuyDate() != null) {
            item.setBuyDate(dto.getBuyDate());
        }

        // 如果没有份额，根据金额和买入净值计算份额
        if ((item.getShares() == null || item.getShares().compareTo(BigDecimal.ZERO) == 0)
                && item.getAmount() != null && item.getBuyNav() != null
                && item.getBuyNav().compareTo(BigDecimal.ZERO) > 0) {
            item.setShares(item.getAmount().divide(item.getBuyNav(), 2, RoundingMode.HALF_UP));
        }

        // 重新计算
        Fund fund = fundService.getByFundCode(item.getFundCode());
        calculateItemValue(item, fund);

        portfolioItemMapper.updateById(item);

        // 刷新组合数据
        refreshPortfolio(portfolioId);

        return convertItemToVO(item);
    }

    @Override
    @Transactional
    public void deleteItem(Long userId, Long portfolioId, Long itemId) {
        getAndCheckOwner(userId, portfolioId);

        PortfolioItem item = portfolioItemMapper.selectById(itemId);
        if (item == null || !item.getPortfolioId().equals(portfolioId)) {
            throw new BusinessException("持仓不存在");
        }

        item.setStatus(0);
        portfolioItemMapper.updateById(item);

        // 刷新组合数据
        refreshPortfolio(portfolioId);
    }

    @Override
    @Transactional
    public void refreshPortfolio(Long portfolioId) {
        Portfolio portfolio = portfolioMapper.selectById(portfolioId);
        if (portfolio == null) {
            return;
        }

        // 获取所有持仓
        LambdaQueryWrapper<PortfolioItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortfolioItem::getPortfolioId, portfolioId)
                .eq(PortfolioItem::getStatus, 1);
        List<PortfolioItem> items = portfolioItemMapper.selectList(wrapper);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal dayProfit = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            Fund fund = fundService.getByFundCode(item.getFundCode());
            if (fund == null) {
                log.warn("基金数据不存在: {}, 尝试获取", item.getFundCode());
                fund = fundService.fetchAndSaveFund(item.getFundCode());
            }

            if (fund != null) {
                log.info("刷新持仓: fundCode={}, dayGrowth={}%, itemAmount={}",
                        item.getFundCode(), fund.getDayGrowth(), item.getAmount());

                calculateItemValue(item, fund);
                portfolioItemMapper.updateById(item);

                totalAmount = totalAmount.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                currentValue = currentValue.add(item.getCurrentValue() != null ? item.getCurrentValue() : BigDecimal.ZERO);
                totalProfit = totalProfit.add(item.getProfit() != null ? item.getProfit() : BigDecimal.ZERO);
                dayProfit = dayProfit.add(item.getDayProfit() != null ? item.getDayProfit() : BigDecimal.ZERO);
            } else {
                log.error("无法获取基金数据: {}", item.getFundCode());
            }
        }

        portfolio.setTotalAmount(totalAmount);
        portfolio.setCurrentValue(currentValue);
        portfolio.setTotalProfit(totalProfit);
        portfolio.setDayProfit(dayProfit);
        portfolio.setFundCount(items.size());

        // 计算收益率
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            portfolio.setTotalReturn(totalProfit.divide(totalAmount, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        }
        // 日收益率基于前一日市值（近似为 currentValue - dayProfit）
        BigDecimal yesterdayValue = currentValue.subtract(dayProfit);
        if (yesterdayValue.compareTo(BigDecimal.ZERO) > 0) {
            portfolio.setDayReturn(dayProfit.divide(yesterdayValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        } else if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            portfolio.setDayReturn(dayProfit.divide(totalAmount, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        } else {
            portfolio.setDayReturn(BigDecimal.ZERO);
        }

        portfolioMapper.updateById(portfolio);
    }

    @Override
    public PortfolioVO getDefaultPortfolio(Long userId) {
        // 先查找默认组合
        LambdaQueryWrapper<Portfolio> defaultWrapper = new LambdaQueryWrapper<>();
        defaultWrapper.eq(Portfolio::getUserId, userId)
                .eq(Portfolio::getIsDefault, 1)
                .eq(Portfolio::getStatus, 1);
        Portfolio portfolio = portfolioMapper.selectOne(defaultWrapper);

        // 如果没有默认组合，查找用户的第一个组合
        if (portfolio == null) {
            LambdaQueryWrapper<Portfolio> firstWrapper = new LambdaQueryWrapper<>();
            firstWrapper.eq(Portfolio::getUserId, userId)
                    .eq(Portfolio::getStatus, 1)
                    .orderByDesc(Portfolio::getUpdateTime)
                    .last("LIMIT 1");
            portfolio = portfolioMapper.selectOne(firstWrapper);

            // 如果找到了，自动设置为默认组合
            if (portfolio != null) {
                portfolio.setIsDefault(1);
                portfolioMapper.updateById(portfolio);
                log.info("自动设置默认组合: userId={}, portfolioId={}", userId, portfolio.getId());
            }
        }

        if (portfolio == null) {
            return null;
        }

        // 刷新数据，确保使用最新净值计算收益
        refreshPortfolio(portfolio.getId());
        portfolio = portfolioMapper.selectById(portfolio.getId());

        return convertToVOWithItems(portfolio);
    }

    @Override
    @Transactional
    public void setDefaultPortfolio(Long userId, Long portfolioId) {
        Portfolio portfolio = getAndCheckOwner(userId, portfolioId);
        portfolio.setIsDefault(1);
        portfolioMapper.updateById(portfolio);
        clearOtherDefault(userId, portfolioId);
    }

    /**
     * 计算持仓市值和盈亏（基于净值精确计算，兼容无份额数据的情况）
     */
    private void calculateItemValue(PortfolioItem item, Fund fund) {
        BigDecimal amount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
        BigDecimal shares = item.getShares();
        BigDecimal currentNav = fund.getNav();

        // 更新当前净值
        if (currentNav != null) {
            item.setCurrentNav(currentNav);
        }

        if (shares != null && shares.compareTo(BigDecimal.ZERO) > 0
                && currentNav != null && currentNav.compareTo(BigDecimal.ZERO) > 0) {
            // 精确模式：基于份额和净值计算
            BigDecimal currentValue = shares.multiply(currentNav)
                    .setScale(2, RoundingMode.HALF_UP);
            item.setCurrentValue(currentValue);

            // 总收益 = 当前市值 - 投资金额
            BigDecimal totalProfit = currentValue.subtract(amount);
            item.setProfit(totalProfit);

            // 总收益率 = 总收益 / 投资金额 × 100
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                item.setProfitRatio(totalProfit.divide(amount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")));
            } else {
                item.setProfitRatio(BigDecimal.ZERO);
            }

            // 今日盈亏 = 当前市值 × 日涨跌幅 / (100 + 日涨跌幅)
            if (fund.getDayGrowth() != null) {
                BigDecimal divisor = new BigDecimal("100").add(fund.getDayGrowth());
                if (divisor.compareTo(BigDecimal.ZERO) != 0) {
                    item.setDayProfit(currentValue.multiply(fund.getDayGrowth())
                            .divide(divisor, 2, RoundingMode.HALF_UP));
                } else {
                    item.setDayProfit(BigDecimal.ZERO);
                }
            } else {
                item.setDayProfit(BigDecimal.ZERO);
            }
        } else {
            // 降级模式：没有份额数据，总收益无法计算
            item.setCurrentValue(amount);
            item.setProfit(BigDecimal.ZERO);
            item.setProfitRatio(BigDecimal.ZERO);

            if (fund.getDayGrowth() != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                item.setDayProfit(amount.multiply(fund.getDayGrowth())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            } else {
                item.setDayProfit(BigDecimal.ZERO);
            }
        }

        log.info("计算收益: fundCode={}, shares={}, nav={}, amount={}, currentValue={}, profit={}, dayProfit={}",
                item.getFundCode(), shares, currentNav, amount,
                item.getCurrentValue(), item.getProfit(), item.getDayProfit());
    }

    /**
     * 清除其他默认组合
     */
    private void clearOtherDefault(Long userId, Long excludeId) {
        LambdaQueryWrapper<Portfolio> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Portfolio::getUserId, userId)
                .ne(Portfolio::getId, excludeId)
                .eq(Portfolio::getIsDefault, 1);

        List<Portfolio> portfolios = portfolioMapper.selectList(wrapper);
        for (Portfolio p : portfolios) {
            p.setIsDefault(0);
            portfolioMapper.updateById(p);
        }
    }

    /**
     * 获取并检查所有权
     */
    private Portfolio getAndCheckOwner(Long userId, Long portfolioId) {
        Portfolio portfolio = portfolioMapper.selectById(portfolioId);
        if (portfolio == null) {
            throw new BusinessException("投资组合不存在");
        }
        if (!portfolio.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此投资组合");
        }
        return portfolio;
    }

    /**
     * 转换为VO
     */
    private PortfolioVO convertToVO(Portfolio portfolio) {
        PortfolioVO vo = new PortfolioVO();
        BeanUtils.copyProperties(portfolio, vo);
        return vo;
    }

    /**
     * 转换为VO（包含持仓列表）
     */
    private PortfolioVO convertToVOWithItems(Portfolio portfolio) {
        PortfolioVO vo = convertToVO(portfolio);

        // 获取持仓列表
        LambdaQueryWrapper<PortfolioItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortfolioItem::getPortfolioId, portfolio.getId())
                .eq(PortfolioItem::getStatus, 1);
        List<PortfolioItem> items = portfolioItemMapper.selectList(wrapper);

        // 批量获取所有需要的 Fund，避免 N 次重复查询
        Map<String, Fund> fundMap = new HashMap<>();
        for (PortfolioItem item : items) {
            if (!fundMap.containsKey(item.getFundCode())) {
                Fund fund = fundService.getByFundCode(item.getFundCode());
                if (fund != null) {
                    fundMap.put(item.getFundCode(), fund);
                }
            }
        }

        List<PortfolioItemVO> itemVOs = items.stream()
                .map(item -> convertItemToVO(item, fundMap))
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        // 计算实际占比和资产配置
        BigDecimal totalValue = vo.getCurrentValue() != null ? vo.getCurrentValue() : BigDecimal.ONE;
        BigDecimal yesterdayProfit = BigDecimal.ZERO;
        List<AssetAllocationVO> allocations = new ArrayList<>();
        int colorIndex = 0;

        for (PortfolioItemVO item : itemVOs) {
            // 计算实际占比
            if (item.getCurrentValue() != null && totalValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = item.getCurrentValue()
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                item.setActualRatio(ratio);
            }

            // 汇总昨日收益
            if (item.getYesterdayProfit() != null) {
                yesterdayProfit = yesterdayProfit.add(item.getYesterdayProfit());
            }

            // 构建资产配置数据
            AssetAllocationVO allocation = new AssetAllocationVO();
            allocation.setType(item.getFundType());
            allocation.setName(item.getFundName());
            allocation.setValue(item.getCurrentValue());
            allocation.setRatio(item.getActualRatio());
            allocation.setColor(COLORS[colorIndex % COLORS.length]);
            allocations.add(allocation);
            colorIndex++;
        }
        vo.setAllocations(allocations);

        // 设置昨日收益
        vo.setYesterdayProfit(yesterdayProfit);
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            vo.setYesterdayReturn(yesterdayProfit.divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        } else {
            vo.setYesterdayReturn(BigDecimal.ZERO);
        }

        return vo;
    }

    /**
     * 转换持仓为VO（便利方法，单个查询）
     */
    private PortfolioItemVO convertItemToVO(PortfolioItem item) {
        Fund fund = fundService.getByFundCode(item.getFundCode());
        Map<String, Fund> fundMap = new HashMap<>();
        if (fund != null) {
            fundMap.put(item.getFundCode(), fund);
        }
        return convertItemToVO(item, fundMap);
    }

    /**
     * 转换持仓为VO（使用预查询的 Fund Map）
     */
    private PortfolioItemVO convertItemToVO(PortfolioItem item, Map<String, Fund> fundMap) {
        PortfolioItemVO vo = new PortfolioItemVO();
        BeanUtils.copyProperties(item, vo);

        Fund fund = fundMap.get(item.getFundCode());
        if (fund != null) {
            vo.setFundType(fund.getFundType());
            vo.setDayGrowth(fund.getDayGrowth());
            vo.setYesterdayGrowth(fund.getYesterdayGrowth());
            if (fund.getYesterdayGrowth() != null && item.getCurrentValue() != null
                    && item.getCurrentValue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal yesterdayProfit = item.getCurrentValue().multiply(fund.getYesterdayGrowth())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                vo.setYesterdayProfit(yesterdayProfit);
            }
        } else {
            log.warn("基金不存在: {}", item.getFundCode());
        }

        return vo;
    }
}
