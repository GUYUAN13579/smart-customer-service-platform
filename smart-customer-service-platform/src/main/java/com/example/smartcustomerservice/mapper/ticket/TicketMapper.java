package com.example.smartcustomerservice.mapper.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartcustomerservice.domain.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

@Mapper
// TicketMapper 属于智能客服平台基础代码。
public interface TicketMapper extends BaseMapper<Ticket> {
}
