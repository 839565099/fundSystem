package com.fund.service;

import com.fund.dto.SectorQueryDTO;
import com.fund.vo.SectorHistoryVO;
import com.fund.vo.SectorStockVO;
import com.fund.vo.SectorVO;
import java.util.List;

/**
 * 板块服务接口
 */
public interface SectorService {

    /**
     * 获取板块列表
     */
    List<SectorVO> getSectors(SectorQueryDTO query);

    /**
     * 获取板块详情
     */
    SectorVO getSectorDetail(String code);

    /**
     * 获取板块历史K线数据
     */
    SectorHistoryVO getSectorHistory(String code, String period);

    /**
     * 获取板块成分股
     */
    List<SectorStockVO> getSectorStocks(String code);

    /**
     * 获取板块类型列表
     */
    List<String> getSectorTypes();
}
