package com.example.smartcustomerservice.domain.vo;

// NotificationUnreadCountVO 属于智能客服平台基础代码。
public class NotificationUnreadCountVO {

    private Long unreadCount;

    public NotificationUnreadCountVO() {
    }

    public NotificationUnreadCountVO(Long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
