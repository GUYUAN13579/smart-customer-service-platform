package com.example.smartcustomerservice.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.smartcustomerservice.common.constants.MqConstants;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.entity.Notification;
import com.example.smartcustomerservice.domain.entity.SlaAlert;
import com.example.smartcustomerservice.domain.entity.Ticket;
import com.example.smartcustomerservice.domain.entity.TicketOperationLog;
import com.example.smartcustomerservice.mapper.notification.NotificationMapper;
import com.example.smartcustomerservice.mapper.sla.SlaAlertMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketOperationLogMapper;
import com.example.smartcustomerservice.mq.message.SlaAlertMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
// SlaAlertMessageConsumer 属于智能客服平台基础代码。
public class SlaAlertMessageConsumer {
    private final SlaAlertMapper slaAlertMapper;
    private final TicketMapper ticketMapper;
    private final NotificationMapper notificationMapper;
    private final TicketOperationLogMapper ticketOperationLogMapper;

    public SlaAlertMessageConsumer(SlaAlertMapper slaAlertMapper,
                                   TicketMapper ticketMapper,
                                   NotificationMapper notificationMapper,
                                   TicketOperationLogMapper ticketOperationLogMapper) {
        this.slaAlertMapper = slaAlertMapper;
        this.ticketMapper = ticketMapper;
        this.notificationMapper = notificationMapper;
        this.ticketOperationLogMapper = ticketOperationLogMapper;
    }

    @RabbitListener(queues = MqConstants.SLA_ALERT_TRIGGER_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void consumeSlaAlert(SlaAlertMessage message) {
        // 消息体只携带 alertId；消费时重新读取告警和工单，不能依赖延迟期间可能已经过期的消息状态。
        if (message == null || message.getAlertId() == null) {
            return;
        }

        SlaAlert slaAlert = slaAlertMapper.selectById(message.getAlertId());
        if (slaAlert == null) {
            return;
        }
        if (!"PENDING".equals(slaAlert.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        // 通过 PENDING -> PROCESSING 条件更新抢占告警。RabbitMQ 重复投递或多实例并发消费时，
        // 只有抢占成功的消费者可以创建通知，从而保证一条告警至多发送一次。
        int claimed = slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                .eq(SlaAlert::getId, slaAlert.getId())
                .eq(SlaAlert::getStatus, "PENDING")
                .set(SlaAlert::getStatus, "PROCESSING")
                .set(SlaAlert::getUpdatedAt, now));
        if (claimed != 1) {
            return;
        }

        Ticket ticket = ticketMapper.selectById(slaAlert.getTicketId());
        if (ticket == null || Integer.valueOf(1).equals(ticket.getDeleted())) {
            markSkipped(slaAlert.getId(), "关联工单不存在或已删除", now);
            return;
        }
        // 工单可能已经响应、解决、关闭或重新应用 SLA；这些旧告警需要标记 SKIPPED 而不是继续提醒。
        if (!shouldNotify(ticket, slaAlert, now)) {
            markSkipped(slaAlert.getId(), "工单当前状态无需发送SLA提醒", now);
            return;
        }
        if (ticket.getAssigneeId() == null) {
            markSkipped(slaAlert.getId(), "工单尚未分配处理人", now);
            return;
        }

        Notification notification = new Notification();
        notification.setReceiverId(ticket.getAssigneeId());
        notification.setType("OVERDUE".equals(slaAlert.getAlertLevel()) ? "SLA_OVERDUE" : "SLA_RISK");
        notification.setTitle(buildNotificationTitle(ticket, slaAlert));
        notification.setContent(buildNotificationContent(ticket, slaAlert));
        notification.setBusinessType("TICKET");
        notification.setBusinessId(ticket.getId());
        notification.setReadStatus("UNREAD");
        notification.setCreatedAt(now);
        notification.setDeleted(0);
        if (notificationMapper.insert(notification) != 1) {
            // 必须继续抛出异常，才能触发 Spring AMQP 的消费重试；吞掉异常会导致告警停留在 PENDING。
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "自动SLA提醒通知插入失败");
        }

        TicketOperationLog operationLog = new TicketOperationLog();
        operationLog.setTicketId(ticket.getId());
        operationLog.setOperationType("SLA_AUTO_ALERT");
        operationLog.setFromStatus(ticket.getStatus());
        operationLog.setToStatus(ticket.getStatus());
        operationLog.setOperationContent("系统自动发送SLA" + getAlertLabel(slaAlert) + "提醒");
        operationLog.setCreatedAt(now);
        operationLog.setDeleted(0);
        if (ticketOperationLogMapper.insert(operationLog) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "自动SLA提醒操作日志插入失败");
        }

        // 通知与操作日志插入成功后才将告警标记 SENT，整个方法的事务保证三者要么都成功、要么都回滚。
        int sent = slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                .eq(SlaAlert::getId, slaAlert.getId())
                .eq(SlaAlert::getStatus, "PROCESSING")
                .set(SlaAlert::getStatus, "SENT")
                .set(SlaAlert::getNotificationId, notification.getId())
                .set(SlaAlert::getSentAt, now)
                .set(SlaAlert::getLastErrorMessage, null)
                .set(SlaAlert::getUpdatedAt, now));
        if (sent != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "SLA告警状态已变更");
        }
    }

    private void markSkipped(Long alertId, String reason, LocalDateTime now) {
        slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                .eq(SlaAlert::getId, alertId)
                .eq(SlaAlert::getStatus, "PROCESSING")
                .set(SlaAlert::getStatus, "SKIPPED")
                .set(SlaAlert::getLastErrorMessage, reason)
                .set(SlaAlert::getUpdatedAt, now));
    }

    private boolean shouldNotify(Ticket ticket, SlaAlert slaAlert, LocalDateTime now) {
        LocalDateTime deadlineAt = "FIRST_RESPONSE".equals(slaAlert.getAlertType())
                ? ticket.getFirstResponseDeadline()
                : ticket.getResolveDeadline();
        if (deadlineAt == null || !deadlineAt.equals(slaAlert.getDeadlineAt())) {
            return false;
        }

        boolean overdue = "OVERDUE".equals(slaAlert.getAlertLevel());
        if ("FIRST_RESPONSE".equals(slaAlert.getAlertType())) {
            return ticket.getFirstRespondedAt() == null
                    && (overdue ? !now.isBefore(deadlineAt) : now.isBefore(deadlineAt));
        }
        if ("RESOLVE".equals(slaAlert.getAlertType())) {
            return !List.of("RESOLVED", "CLOSED", "REJECTED").contains(ticket.getStatus())
                    && (overdue ? !now.isBefore(deadlineAt) : now.isBefore(deadlineAt));
        }
        return false;
    }

    private String buildNotificationTitle(Ticket ticket, SlaAlert slaAlert) {
        return "SLA" + getAlertLabel(slaAlert) + "提醒：" + ticket.getTicketNo();
    }

    private String buildNotificationContent(Ticket ticket, SlaAlert slaAlert) {
        return "工单 " + ticket.getTicketNo()
                + " 的" + getAlertLabel(slaAlert)
                + "，截止时间：" + slaAlert.getDeadlineAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + "，请及时处理。";
    }

    private String getAlertLabel(SlaAlert slaAlert) {
        String typeLabel = "FIRST_RESPONSE".equals(slaAlert.getAlertType()) ? "首次响应" : "解决";
        String levelLabel = "OVERDUE".equals(slaAlert.getAlertLevel()) ? "已超时" : "即将超时";
        return typeLabel + levelLabel;
    }
}
