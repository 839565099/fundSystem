package com.fund.service.impl;

import com.fund.entity.FundManager;
import com.fund.external.FundDataApiService;
import com.fund.mapper.FundManagerMapper;
import com.fund.service.FundManagerService;
import com.fund.vo.FundManagerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundManagerServiceImpl implements FundManagerService {

    private final FundManagerMapper fundManagerMapper;
    private final FundDataApiService fundDataApiService;

    @Override
    public List<FundManagerVO> getManagersByFundCode(String fundCode) {
        // 1. 先从数据库获取
        List<FundManager> managers = fundManagerMapper.getManagersByFundCode(fundCode);

        // 2. 数据库为空或数据不完整时，从东方财富获取
        boolean needRefresh = (managers == null || managers.isEmpty());
        if (!needRefresh && managers.stream().anyMatch(m -> m.getCompany() == null)) {
            needRefresh = true;
            log.info("基金经理数据不完整(缺少company)，触发刷新: {}", fundCode);
        }
        if (needRefresh) {
            log.info("数据库中无基金经理数据，从东方财富获取: {}", fundCode);
            managers = fundDataApiService.fetchFundManagers(fundCode);

            // 3. 缓存到数据库
            if (managers != null && !managers.isEmpty()) {
                for (FundManager manager : managers) {
                    try {
                        // 保存或更新经理信息
                        fundManagerMapper.insertOrUpdate(manager);
                        // 建立关联关系
                        fundManagerMapper.insertRelation(fundCode, manager.getManagerId(), manager.getStartDate());
                        log.info("保存基金经理: {} - {}", fundCode, manager.getManagerName());
                    } catch (Exception e) {
                        log.warn("保存基金经理失败: {} - {}", fundCode, manager.getManagerName(), e);
                    }
                }
            }
        }

        if (managers == null) {
            return new ArrayList<>();
        }

        return managers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public FundManager getByManagerId(String managerId) {
        return fundManagerMapper.selectById(managerId);
    }

    private FundManagerVO convertToVO(FundManager manager) {
        FundManagerVO vo = new FundManagerVO();
        BeanUtils.copyProperties(manager, vo);
        // 当结构化接口字段缺失时，从简历文本中提取基础从业年限，避免前端关键信息为空
        if (vo.getWorkYears() == null && vo.getResume() != null) {
            Matcher matcher = Pattern.compile("(\\d+)年").matcher(vo.getResume());
            if (matcher.find()) {
                try {
                    vo.setWorkYears(Integer.parseInt(matcher.group(1)));
                } catch (Exception ignored) {
                }
            }
        }
        return vo;
    }
}
