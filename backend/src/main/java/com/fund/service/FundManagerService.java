package com.fund.service;

import com.fund.entity.FundManager;
import com.fund.vo.FundManagerVO;
import java.util.List;

public interface FundManagerService {
    
    List<FundManagerVO> getManagersByFundCode(String fundCode);
    
    FundManager getByManagerId(String managerId);
}
