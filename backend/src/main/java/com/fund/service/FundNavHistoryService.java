package com.fund.service;

import com.fund.entity.FundNavHistory;
import com.fund.vo.FundNavHistoryVO;
import java.time.LocalDate;
import java.util.List;

public interface FundNavHistoryService {
    
    List<FundNavHistoryVO> getNavHistory(String fundCode, String period);
    
    List<FundNavHistory> getHistoryByDateRange(String fundCode, LocalDate startDate);
    
    List<FundNavHistory> getRecentHistory(String fundCode, int limit);
    
    void saveNavHistory(FundNavHistory navHistory);
    
    void batchSaveNavHistory(List<FundNavHistory> navHistoryList);
}
