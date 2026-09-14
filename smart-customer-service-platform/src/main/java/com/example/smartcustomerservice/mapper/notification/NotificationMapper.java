package com.example.smartcustomerservice.mapper.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartcustomerservice.domain.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
// NotificationMapper 属于智能客服平台基础代码。
public interface NotificationMapper extends BaseMapper<Notification> {
}
