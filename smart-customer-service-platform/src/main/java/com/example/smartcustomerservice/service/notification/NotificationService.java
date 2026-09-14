package com.example.smartcustomerservice.service.notification;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.NotificationQueryRequest;
import com.example.smartcustomerservice.domain.vo.NotificationUnreadCountVO;
import com.example.smartcustomerservice.domain.vo.NotificationVO;

// NotificationService 属于智能客服平台基础代码。
public interface NotificationService {

    PageResult<NotificationVO> pageNotifications(NotificationQueryRequest request);

    NotificationUnreadCountVO getUnreadCount();

    Boolean markAsRead(Long id);

    Boolean markAllAsRead();
}
