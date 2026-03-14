package com.fund.service.impl;

import com.fund.entity.FundManager;
import com.fund.mapper.FundManagerMapper;
import com.fund.service.FundManagerService;
import com.fund.vo.FundManagerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundManagerServiceImpl implements FundManagerService {
    
    private final FundManagerMapper fundManagerMapper;
    
    @Override
    public List<FundManagerVO> getManagersByFundCode(String fundCode) {
        List<FundManager> managers = fundManagerMapper.getManagersByFundCode(fundCode);
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
        return vo;
    }
}
