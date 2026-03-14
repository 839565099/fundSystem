package com.fund.service;

import com.fund.entity.Fund;
import java.util.List;

public interface HotFundService {
    
    List<Fund> getHotFunds(int limit);
    
    void addHotFund(String fundCode, Integer sortNum);
    
    void removeHotFund(String fundCode);
    
    void updateSortNum(String fundCode, Integer sortNum);
    
    boolean isHotFund(String fundCode);
}
