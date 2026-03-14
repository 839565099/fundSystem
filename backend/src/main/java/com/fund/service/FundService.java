package com.fund.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.dto.FundSearchDTO;
import com.fund.entity.Fund;
import com.fund.vo.FundDetailVO;
import com.fund.vo.FundListVO;
import java.util.List;

public interface FundService {
    
    Page<FundListVO> searchFunds(FundSearchDTO searchDTO);
    
    List<Fund> searchByKeyword(String keyword, int limit);
    
    FundDetailVO getFundDetail(String fundCode);
    
    List<Fund> getHotFunds(int limit);
    
    List<Fund> getTopGrowthFunds(int limit);
    
    Fund getByFundCode(String fundCode);
    
    void updateFundData(String fundCode);
    
    void batchUpdateFundData();

    Page<FundListVO> getRankingList(String rankingType, String period, Integer pageNum, Integer pageSize);

    List<String> getFundTypes();

    List<String> getFundCompanies();
}
