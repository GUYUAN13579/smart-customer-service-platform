package com.example.smartcustomerservice.service.impl.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.NotificationQueryRequest;
import com.example.smartcustomerservice.domain.entity.Notification;
import com.example.smartcustomerservice.domain.vo.NotificationUnreadCountVO;
import com.example.smartcustomerservice.domain.vo.NotificationVO;
import com.example.smartcustomerservice.mapper.notification.NotificationMapper;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.notification.NotificationService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
// NotificationServiceImpl 属于智能客服平台基础代码。
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public PageResult<NotificationVO> pageNotifications(NotificationQueryRequest request) {
        // TODO: 分页查询当前登录用户的通知并转换为返回对象。
        Long currentId = SecurityUtils.getCurrentUserId();
        if (currentId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前登录用户");
        }

        NotificationQueryRequest safeRequest = request == null ? new NotificationQueryRequest() : request;
        Page<Notification> notificationPage = new Page<>(safeRequest.getPage(), safeRequest.getSize());
        // receiverId 固定为当前登录用户，不能由前端传入，否则会造成越权查看通知。
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, currentId)
                .eq(Notification::getDeleted, 0)
                .eq(StringUtils.hasText(safeRequest.getType()), Notification::getType, safeRequest.getType())
                .eq(StringUtils.hasText(safeRequest.getBusinessType()), Notification::getBusinessType, safeRequest.getBusinessType())
                .eq(StringUtils.hasText(safeRequest.getReadStatus()), Notification::getReadStatus, safeRequest.getReadStatus())
                .orderByDesc(Notification::getCreatedAt)
                .orderByDesc(Notification::getId);
        Page<Notification> notificationPage1 = notificationMapper.selectPage(notificationPage, wrapper);
        List<Notification> notificationList = notificationPage1.getRecords();
        List<NotificationVO> voList = notificationList.stream().map(
                entity ->{
                    NotificationVO notificationVO = new NotificationVO();
                    BeanUtils.copyProperties(entity, notificationVO);
                    return notificationVO;
                }
        ).toList();

        return PageResult.of(voList, notificationPage1.getCurrent(),
                notificationPage1.getSize(), notificationPage1.getTotal());
    }

    @Override
    public NotificationUnreadCountVO getUnreadCount() {
        // TODO: 统计当前登录用户的未读通知数量。
        Long currentId = SecurityUtils.getCurrentUserId();
        if (currentId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前登录用户");
        }
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, currentId)
                .eq(Notification::getDeleted, 0)
                .eq(Notification::getReadStatus, "UNREAD"));
        NotificationUnreadCountVO notificationUnreadCountVO = new NotificationUnreadCountVO();
        notificationUnreadCountVO.setUnreadCount(count);
        return notificationUnreadCountVO;
    }

    @Override
    public Boolean markAsRead(Long id) {
        // TODO: 将当前登录用户指定的通知标记为已读。
        Long currentId = SecurityUtils.getCurrentUserId();
        if (currentId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前登录用户");
        }
        Notification notification = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getReceiverId, currentId)
                .eq(Notification::getDeleted, 0));
        if (notification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        if ("READ".equals(notification.getReadStatus())) {
            return true;
        }

        // 仅将仍为 UNREAD 的记录更新为 READ，使重复点击保持幂等并规避并发覆盖。
        int updated = notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getReceiverId, currentId)
                .eq(Notification::getDeleted, 0)
                .eq(Notification::getReadStatus, "UNREAD")
                .set(Notification::getReadStatus, "READ")
                .set(Notification::getReadAt, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "通知状态已变更，请刷新后重试");
        }
        return true;
    }

    @Override
    public Boolean markAllAsRead() {
        Long currentId = SecurityUtils.getCurrentUserId();
        if (currentId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前登录用户");
        }
        // 批量更新同样受 receiverId 限制，当前用户只能处理自己的未读通知。
        notificationMapper.update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getReceiverId, currentId)
                .eq(Notification::getDeleted, 0)
                .eq(Notification::getReadStatus, "UNREAD")
                .set(Notification::getReadStatus, "READ")
                .set(Notification::getReadAt, LocalDateTime.now()));
        return true;
    }
}
