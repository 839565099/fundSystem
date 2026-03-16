package com.fund.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.dto.PortfolioDTO;
import com.fund.dto.PortfolioItemDTO;
import com.fund.entity.Fund;
import com.fund.entity.Portfolio;
import com.fund.entity.PortfolioItem;
import com.fund.mapper.PortfolioItemMapper;
import com.fund.mapper.PortfolioMapper;
import com.fund.service.impl.PortfolioServiceImpl;
import com.fund.vo.PortfolioVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 投资组合服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioMapper portfolioMapper;

    @Mock
    private PortfolioItemMapper portfolioItemMapper;

    @Mock
    private FundService fundService;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private Portfolio testPortfolio;
    private PortfolioDTO portfolioDTO;
    private Fund testFund;

    @BeforeEach
    void setUp() {
        // 准备测试组合
        testPortfolio = new Portfolio();
        testPortfolio.setId(1L);
        testPortfolio.setUserId(1L);
        testPortfolio.setName("测试组合");
        testPortfolio.setDescription("这是一个测试组合");
        testPortfolio.setTotalAmount(BigDecimal.ZERO);
        testPortfolio.setCurrentValue(BigDecimal.ZERO);
        testPortfolio.setFundCount(0);
        testPortfolio.setStatus(1);
        testPortfolio.setIsDefault(0);

        // 准备组合 DTO
        portfolioDTO = new PortfolioDTO();
        portfolioDTO.setName("新组合");
        portfolioDTO.setDescription("新组合描述");
        portfolioDTO.setIsDefault(1);

        // 准备测试基金
        testFund = new Fund();
        testFund.setFundCode("000001");
        testFund.setFundName("华夏成长");
        testFund.setNav(new BigDecimal("1.5"));
        testFund.setDayGrowth(new BigDecimal("1.5"));
    }

    @Test
    @DisplayName("创建组合成功")
    void createPortfolio_success() {
        // given
        when(portfolioMapper.insert(any(Portfolio.class))).thenAnswer(invocation -> {
            Portfolio p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });
        when(portfolioMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // when
        PortfolioVO result = portfolioService.createPortfolio(1L, portfolioDTO);

        // then
        assertNotNull(result);
        assertEquals(portfolioDTO.getName(), result.getName());
        verify(portfolioMapper, times(1)).insert(any(Portfolio.class));
    }

    @Test
    @DisplayName("创建默认组合 - 清除其他默认")
    void createPortfolio_asDefault() {
        // given
        Portfolio existingDefault = new Portfolio();
        existingDefault.setId(2L);
        existingDefault.setIsDefault(1);

        when(portfolioMapper.insert(any(Portfolio.class))).thenAnswer(invocation -> {
            Portfolio p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });
        when(portfolioMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(existingDefault));
        when(portfolioMapper.updateById(any(Portfolio.class))).thenReturn(1);

        // when
        PortfolioVO result = portfolioService.createPortfolio(1L, portfolioDTO);

        // then
        assertNotNull(result);
        verify(portfolioMapper, times(1)).updateById(any(Portfolio.class)); // 清除其他默认
    }

    @Test
    @DisplayName("更新组合成功")
    void updatePortfolio_success() {
        // given
        when(portfolioMapper.selectById(1L)).thenReturn(testPortfolio);
        when(portfolioMapper.updateById(any(Portfolio.class))).thenReturn(1);

        PortfolioDTO updateDTO = new PortfolioDTO();
        updateDTO.setName("更新后的名称");

        // when
        PortfolioVO result = portfolioService.updatePortfolio(1L, 1L, updateDTO);

        // then
        assertNotNull(result);
        assertEquals("更新后的名称", result.getName());
    }

    @Test
    @DisplayName("更新组合失败 - 组合不存在")
    void updatePortfolio_notFound() {
        // given
        when(portfolioMapper.selectById(999L)).thenReturn(null);

        // when & then
        assertThrows(RuntimeException.class,
            () -> portfolioService.updatePortfolio(1L, 999L, portfolioDTO));
    }

    @Test
    @DisplayName("更新组合失败 - 无权限")
    void updatePortfolio_noPermission() {
        // given
        testPortfolio.setUserId(2L); // 不同用户
        when(portfolioMapper.selectById(1L)).thenReturn(testPortfolio);

        // when & then
        assertThrows(RuntimeException.class,
            () -> portfolioService.updatePortfolio(1L, 1L, portfolioDTO));
    }

    @Test
    @DisplayName("删除组合成功")
    void deletePortfolio_success() {
        // given
        when(portfolioMapper.selectById(1L)).thenReturn(testPortfolio);
        when(portfolioItemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        when(portfolioMapper.deleteById(1L)).thenReturn(1);

        // when
        portfolioService.deletePortfolio(1L, 1L);

        // then
        verify(portfolioItemMapper, times(1)).delete(any(LambdaQueryWrapper.class));
        verify(portfolioMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("获取用户组合列表")
    void getUserPortfolios_success() {
        // given
        List<Portfolio> portfolios = new ArrayList<>();
        portfolios.add(testPortfolio);

        when(portfolioMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(portfolios);

        // when
        List<PortfolioVO> result = portfolioService.getUserPortfolios(1L);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试组合", result.get(0).getName());
    }

    @Test
    @DisplayName("获取组合详情成功")
    void getPortfolioDetail_success() {
        // given
        when(portfolioMapper.selectById(1L)).thenReturn(testPortfolio);
        when(portfolioItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // when
        PortfolioVO result = portfolioService.getPortfolioDetail(1L, 1L);

        // then
        assertNotNull(result);
        assertEquals("测试组合", result.getName());
    }

    @Test
    @DisplayName("添加持仓成功")
    void addItem_success() {
        // given
        PortfolioItemDTO itemDTO = new PortfolioItemDTO();
        itemDTO.setFundCode("000001");
        itemDTO.setAmount(new BigDecimal("10000"));
        itemDTO.setShares(new BigDecimal("6666.67"));

        when(portfolioMapper.selectById(1L)).thenReturn(testPortfolio);
        when(fundService.getByFundCode("000001")).thenReturn(testFund);
        when(portfolioItemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(portfolioItemMapper.insert(any(PortfolioItem.class))).thenReturn(1);
        when(portfolioItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(portfolioMapper.updateById(any(Portfolio.class))).thenReturn(1);

        // when
        com.fund.vo.PortfolioItemVO result = portfolioService.addItem(1L, 1L, itemDTO);

        // then
        assertNotNull(result);
        assertEquals("000001", result.getFundCode());
    }

    @Test
    @DisplayName("添加持仓失败 - 基金不存在")
    void addItem_fundNotFound() {
        // given
        PortfolioItemDTO itemDTO = new PortfolioItemDTO();
        itemDTO.setFundCode("999999");

        when(portfolioMapper.selectById(1L)).thenReturn(testPortfolio);
        when(fundService.getByFundCode("999999")).thenReturn(null);

        // when & then
        assertThrows(RuntimeException.class,
            () -> portfolioService.addItem(1L, 1L, itemDTO));
    }

    @Test
    @DisplayName("添加持仓失败 - 基金已存在")
    void addItem_fundAlreadyExists() {
        // given
        PortfolioItemDTO itemDTO = new PortfolioItemDTO();
        itemDTO.setFundCode("000001");

        when(portfolioMapper.selectById(1L)).thenReturn(testPortfolio);
        when(fundService.getByFundCode("000001")).thenReturn(testFund);
        when(portfolioItemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // when & then
        assertThrows(RuntimeException.class,
            () -> portfolioService.addItem(1L, 1L, itemDTO));
    }

    @Test
    @DisplayName("设置默认组合成功")
    void setDefaultPortfolio_success() {
        // given
        Portfolio existingDefault = new Portfolio();
        existingDefault.setId(2L);
        existingDefault.setIsDefault(1);

        when(portfolioMapper.selectById(1L)).thenReturn(testPortfolio);
        when(portfolioMapper.updateById(any(Portfolio.class))).thenReturn(1);
        when(portfolioMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(existingDefault));

        // when
        portfolioService.setDefaultPortfolio(1L, 1L);

        // then
        verify(portfolioMapper, atLeast(1)).updateById(any(Portfolio.class));
    }
}
