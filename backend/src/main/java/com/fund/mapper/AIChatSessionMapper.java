package com.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.entity.AIChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AIChatSessionMapper extends BaseMapper<AIChatSession> {

    @Select("SELECT * FROM t_ai_chat_session WHERE session_id = #{sessionId}")
    AIChatSession selectBySessionId(String sessionId);
}
