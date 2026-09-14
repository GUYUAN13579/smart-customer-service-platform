package com.example.smartcustomerservice.mapper.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartcustomerservice.domain.entity.TicketProcessRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
// TicketProcessRecordMapper 属于智能客服平台基础代码。
public interface TicketProcessRecordMapper extends BaseMapper<TicketProcessRecord> {
}
