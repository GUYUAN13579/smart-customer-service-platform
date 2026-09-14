package com.example.smartcustomerservice.service.impl.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.SlaTicketMonitorQueryRequest;
import com.example.smartcustomerservice.domain.dto.SlaTicketRemindRequest;
import com.example.smartcustomerservice.domain.entity.Notification;
import com.example.smartcustomerservice.domain.entity.SysUser;
import com.example.smartcustomerservice.domain.entity.Ticket;
import com.example.smartcustomerservice.domain.entity.TicketOperationLog;
import com.example.smartcustomerservice.domain.vo.SlaTicketMonitorVO;
import com.example.smartcustomerservice.domain.vo.SlaTicketRemindVO;
import com.example.smartcustomerservice.mapper.notification.NotificationMapper;
import com.example.smartcustomerservice.mapper.auth.SysUserMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketOperationLogMapper;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.sla.SlaMonitorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
// SlaMonitorServiceImpl 属于智能客服平台基础代码。
public class SlaMonitorServiceImpl implements SlaMonitorService {

    private final TicketMapper ticketMapper;
    private final NotificationMapper notificationMapper;
    private final TicketOperationLogMapper ticketOperationLogMapper;
    private final SysUserMapper sysUserMapper;

    public SlaMonitorServiceImpl(TicketMapper ticketMapper,
                                 NotificationMapper notificationMapper,
                                 TicketOperationLogMapper ticketOperationLogMapper,
                                 SysUserMapper sysUserMapper) {
        this.ticketMapper = ticketMapper;
        this.notificationMapper = notificationMapper;
        this.ticketOperationLogMapper = ticketOperationLogMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public PageResult<SlaTicketMonitorVO> pageRiskTickets(SlaTicketMonitorQueryRequest request) {
        // TODO: 查询即将违反首次响应或解决时限的工单。
        if(request == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求不存在");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadlineBefore = now.plusMinutes(request.getWithinMinutes() == null ? 30L : request.getWithinMinutes());
        Page<Ticket> logPage = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getDeleted, 0);

        if("FIRST_RESPONSE".equals(request.getType())) {
            wrapper.between(Ticket::getFirstResponseDeadline, now, deadlineBefore);
        } else if("RESOLVE".equals(request.getType())) {
            wrapper.between(Ticket::getResolveDeadline, now, deadlineBefore);
        } else {
            // 显式分组为 (首响截止在窗口内 OR 解决截止在窗口内)，避免 OR 改写前面的 deleted=0 条件。
            wrapper.and(item -> item
                    .between(Ticket::getFirstResponseDeadline, now, deadlineBefore)
                    .or()
                    .between(Ticket::getResolveDeadline, now, deadlineBefore));
        }

        wrapper.eq(StringUtils.hasText(request.getCategory()), Ticket::getCategory, request.getCategory())
                .eq(StringUtils.hasText(request.getPriority()), Ticket::getPriority, request.getPriority())
                .eq(request.getAssigneeId() != null, Ticket::getAssigneeId, request.getAssigneeId());

        Page<Ticket> ticketList = ticketMapper.selectPage(logPage, wrapper);
        List<SlaTicketMonitorVO> slaTicketMonitorVOS = ticketList.getRecords().stream()
                .map(
                        entity ->{
                            SlaTicketMonitorVO slaTicketMonitorVO = new SlaTicketMonitorVO();
                            slaTicketMonitorVO.setTicketId(entity.getId());
                            slaTicketMonitorVO.setTicketNo(entity.getTicketNo());
                            slaTicketMonitorVO.setTitle(entity.getTitle());
                            slaTicketMonitorVO.setStatus(entity.getStatus());
                            slaTicketMonitorVO.setAssigneeId(entity.getAssigneeId());
                            slaTicketMonitorVO.setCategory(entity.getCategory());
                            slaTicketMonitorVO.setPriority(entity.getPriority());
                            slaTicketMonitorVO.setFirstResponseDeadline(entity.getFirstResponseDeadline());
                            slaTicketMonitorVO.setResolveDeadline(entity.getResolveDeadline());
                            slaTicketMonitorVO.setFirstRespondedAt(entity.getFirstRespondedAt());
                            slaTicketMonitorVO.setCreatedAt(entity.getCreatedAt());
                            slaTicketMonitorVO.setUpdatedAt(entity.getUpdatedAt());

                            String riskType = StringUtils.hasText(request.getType()) ? request.getType() : "FIRST_RESPONSE";
                            LocalDateTime deadlineAt = "RESOLVE".equals(riskType)
                                    ? entity.getResolveDeadline()
                                    : entity.getFirstResponseDeadline();

                            slaTicketMonitorVO.setRiskType(riskType);
                            slaTicketMonitorVO.setDeadlineAt(deadlineAt);
                            if(deadlineAt != null) {
                                slaTicketMonitorVO.setRemainingMinutes(Duration.between(now, deadlineAt).toMinutes());
                            }
                            slaTicketMonitorVO.setOverdueMinutes(null);
                            return slaTicketMonitorVO;
                        }

                )
                .toList();
        return PageResult.of(slaTicketMonitorVOS, ticketList.getCurrent(), ticketList.getSize(), ticketList.getTotal());

    }

    @Override
    public PageResult<SlaTicketMonitorVO> pageOverdueTickets(SlaTicketMonitorQueryRequest request) {
        // TODO: 查询已经违反首次响应或解决时限的工单。
        if(request == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求不存在");
        LocalDateTime now = LocalDateTime.now();
        Page<Ticket> logPage = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getDeleted, 0);

        if("FIRST_RESPONSE".equals(request.getType())) {
            wrapper.isNull(Ticket::getFirstRespondedAt)
                    .lt(Ticket::getFirstResponseDeadline, now);
        } else if("RESOLVE".equals(request.getType())) {
            wrapper.notIn(Ticket::getStatus, "RESOLVED", "CLOSED", "REJECTED")
                    .lt(Ticket::getResolveDeadline, now);
        } else {
            // 两类超时条件各自保留完成状态判断，再组合成一个括号内的 OR 条件。
            wrapper.and(item -> item
                    .and(first -> first
                            .isNull(Ticket::getFirstRespondedAt)
                            .lt(Ticket::getFirstResponseDeadline, now))
                    .or()
                    .and(resolve -> resolve
                            .notIn(Ticket::getStatus, "RESOLVED", "CLOSED", "REJECTED")
                            .lt(Ticket::getResolveDeadline, now)));
        }

        wrapper.eq(StringUtils.hasText(request.getCategory()), Ticket::getCategory, request.getCategory())
                .eq(StringUtils.hasText(request.getPriority()), Ticket::getPriority, request.getPriority())
                .eq(request.getAssigneeId() != null, Ticket::getAssigneeId, request.getAssigneeId());

        Page<Ticket> ticketList = ticketMapper.selectPage(logPage, wrapper);
        List<SlaTicketMonitorVO> slaTicketMonitorVOS = ticketList.getRecords().stream()
                .map(
                        entity ->{
                            SlaTicketMonitorVO slaTicketMonitorVO = new SlaTicketMonitorVO();
                            slaTicketMonitorVO.setTicketId(entity.getId());
                            slaTicketMonitorVO.setTicketNo(entity.getTicketNo());
                            slaTicketMonitorVO.setTitle(entity.getTitle());
                            slaTicketMonitorVO.setStatus(entity.getStatus());
                            slaTicketMonitorVO.setAssigneeId(entity.getAssigneeId());
                            slaTicketMonitorVO.setCategory(entity.getCategory());
                            slaTicketMonitorVO.setPriority(entity.getPriority());
                            slaTicketMonitorVO.setFirstResponseDeadline(entity.getFirstResponseDeadline());
                            slaTicketMonitorVO.setResolveDeadline(entity.getResolveDeadline());
                            slaTicketMonitorVO.setFirstRespondedAt(entity.getFirstRespondedAt());
                            slaTicketMonitorVO.setCreatedAt(entity.getCreatedAt());
                            slaTicketMonitorVO.setUpdatedAt(entity.getUpdatedAt());

                            String riskType = StringUtils.hasText(request.getType()) ? request.getType() : "FIRST_RESPONSE";
                            LocalDateTime deadlineAt = "RESOLVE".equals(riskType)
                                    ? entity.getResolveDeadline()
                                    : entity.getFirstResponseDeadline();

                            slaTicketMonitorVO.setRiskType(riskType);
                            slaTicketMonitorVO.setDeadlineAt(deadlineAt);
                            slaTicketMonitorVO.setRemainingMinutes(null);
                            if(deadlineAt != null) {
                                slaTicketMonitorVO.setOverdueMinutes(Duration.between(deadlineAt, now).toMinutes());
                            }
                            return slaTicketMonitorVO;
                        }

                )
                .toList();
        return PageResult.of(slaTicketMonitorVOS, ticketList.getCurrent(), ticketList.getSize(), ticketList.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SlaTicketRemindVO remindTicket(Long ticketId, SlaTicketRemindRequest request) {
        // TODO: 校验工单并为指定接收人创建 SLA 通知和工单操作日志。
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求参数错误");
        }

        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || Integer.valueOf(CommonConstants.DELETED).equals(ticket.getDeleted())) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        }
        if ("CLOSED".equals(ticket.getStatus()) || "REJECTED".equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "当前工单不允许发送SLA提醒");
        }
        if (request.getRemindType().startsWith("FIRST_RESPONSE") && ticket.getFirstRespondedAt() != null) {
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "工单已完成首次响应，无需发送首响提醒");
        }
        if (request.getRemindType().startsWith("RESOLVE") && "RESOLVED".equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "工单已解决，无需发送解决提醒");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前登录用户");
        }

        SysUser receiver = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, request.getReceiverId())
                .eq(SysUser::getDeleted, CommonConstants.NOT_DELETED));
        if (receiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提醒接收人不存在");
        }
        if (!CommonConstants.ENABLED.equals(receiver.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED, "提醒接收人已被禁用");
        }

        LocalDateTime now = LocalDateTime.now();
        String notificationContent = StringUtils.hasText(request.getContent())
                ? request.getContent()
                : "工单 " + ticket.getTicketNo() + " 存在SLA风险，请及时处理。";

        // 通知与操作日志处于同一事务，避免出现“已提醒但没有审计记录”的不一致状态。
        Notification notification = new Notification();
        notification.setReceiverId(request.getReceiverId());
        notification.setTitle("SLA提醒：" + ticket.getTicketNo());
        notification.setContent(notificationContent);
        notification.setBusinessId(ticket.getId());
        notification.setType(
                request.getRemindType().contains("OVERDUE")
                        ? "SLA_OVERDUE"
                        : "SLA_RISK"
        );
        notification.setBusinessType("TICKET");
        notification.setReadStatus("UNREAD");
        notification.setCreatedAt(now);
        notification.setDeleted(CommonConstants.NOT_DELETED);
        int notificationInserted = notificationMapper.insert(notification);
        if (notificationInserted != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "通知插入失败");
        }

        TicketOperationLog ticketOperationLog = new TicketOperationLog();
        ticketOperationLog.setTicketId(ticket.getId());
        ticketOperationLog.setOperatorId(currentUserId);
        ticketOperationLog.setOperationType("SLA_REMIND");
        ticketOperationLog.setFromStatus(ticket.getStatus());
        ticketOperationLog.setToStatus(ticket.getStatus());
        ticketOperationLog.setOperationContent(
                "发送SLA提醒，类型：" + request.getRemindType()
                        + "，接收人ID：" + request.getReceiverId()
                        + "，提醒内容：" + notificationContent
        );
        ticketOperationLog.setCreatedAt(now);
        ticketOperationLog.setDeleted(CommonConstants.NOT_DELETED);

        int inserted = ticketOperationLogMapper.insert(ticketOperationLog);
        if (inserted != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "工单操作日志插入失败");
        }

        SlaTicketRemindVO vo = new SlaTicketRemindVO();
        vo.setNotificationId(notification.getId());
        vo.setTicketId(ticket.getId());
        vo.setReceiverId(notification.getReceiverId());
        vo.setRemindType(request.getRemindType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setCreatedAt(now);
        return vo;
    }
}
