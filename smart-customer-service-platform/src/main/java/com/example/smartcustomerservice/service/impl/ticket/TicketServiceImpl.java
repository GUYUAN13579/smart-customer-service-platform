package com.example.smartcustomerservice.service.impl.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.event.ticket.TicketApprovedEvent;
import com.example.smartcustomerservice.domain.dto.ConversationManualTransferRequest;
import com.example.smartcustomerservice.domain.dto.TicketApplySlaRequest;
import com.example.smartcustomerservice.domain.dto.TicketAssignRequest;
import com.example.smartcustomerservice.domain.dto.TicketCloseRequest;
import com.example.smartcustomerservice.domain.dto.TicketOperationLogQueryRequest;
import com.example.smartcustomerservice.domain.dto.TicketProcessRecordCreateRequest;
import com.example.smartcustomerservice.domain.dto.TicketProcessRecordQueryRequest;
import com.example.smartcustomerservice.domain.dto.TicketQueryRequest;
import com.example.smartcustomerservice.domain.dto.TicketReviewRequest;
import com.example.smartcustomerservice.domain.dto.TicketResolveRequest;
import com.example.smartcustomerservice.domain.dto.TicketStartRequest;
import com.example.smartcustomerservice.domain.entity.*;
import com.example.smartcustomerservice.domain.vo.TicketApplySlaVO;
import com.example.smartcustomerservice.domain.vo.TicketAutoAssignPreviewVO;
import com.example.smartcustomerservice.domain.vo.SkillGroupAgentLoadVO;
import com.example.smartcustomerservice.domain.vo.TicketFullDetailVO;
import com.example.smartcustomerservice.domain.vo.TicketOperationLogVO;
import com.example.smartcustomerservice.domain.vo.TicketProcessRecordVO;
import com.example.smartcustomerservice.domain.vo.TicketVO;
import com.example.smartcustomerservice.mapper.conversation.ConversationMessageMapper;
import com.example.smartcustomerservice.mapper.conversation.ConversationSessionMapper;
import com.example.smartcustomerservice.mapper.assignment.AssignmentRuleMapper;
import com.example.smartcustomerservice.mapper.notification.NotificationMapper;
import com.example.smartcustomerservice.mapper.sla.SlaAlertMapper;
import com.example.smartcustomerservice.mapper.sla.SlaPolicyMapper;
import com.example.smartcustomerservice.mapper.skill.SkillGroupMapper;
import com.example.smartcustomerservice.mapper.skill.SkillGroupMemberMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketOperationLogMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketProcessRecordMapper;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.mq.message.SlaAlertMessage;
import com.example.smartcustomerservice.mq.producer.SlaAlertMessageProducer;
import com.example.smartcustomerservice.service.ticket.TicketService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
// TicketServiceImpl 属于智能客服平台基础代码。
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final ConversationSessionMapper conversationSessionMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final TicketProcessRecordMapper ticketProcessRecordMapper;
    private final TicketOperationLogMapper ticketOperationLogMapper;
    private final SlaPolicyMapper slaPolicyMapper;
    private final SlaAlertMapper slaAlertMapper;
    private final SlaAlertMessageProducer slaAlertMessageProducer;
    private final AssignmentRuleMapper assignmentRuleMapper;
    private final SkillGroupMapper skillGroupMapper;
    private final SkillGroupMemberMapper skillGroupMemberMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationMapper notificationMapper;

    public TicketServiceImpl(TicketMapper ticketMapper,
                             ConversationSessionMapper conversationSessionMapper,
                             ConversationMessageMapper conversationMessageMapper,
                             TicketProcessRecordMapper ticketProcessRecordMapper,
                             TicketOperationLogMapper ticketOperationLogMapper,
                             SlaPolicyMapper slaPolicyMapper,
                             SlaAlertMapper slaAlertMapper,
                             SlaAlertMessageProducer slaAlertMessageProducer,
                             AssignmentRuleMapper assignmentRuleMapper,
                             SkillGroupMapper skillGroupMapper,
                             SkillGroupMemberMapper skillGroupMemberMapper,
                             ApplicationEventPublisher applicationEventPublisher,
                             NotificationMapper notificationMapper) {
        this.ticketMapper = ticketMapper;
        this.conversationSessionMapper = conversationSessionMapper;
        this.conversationMessageMapper = conversationMessageMapper;
        this.ticketProcessRecordMapper = ticketProcessRecordMapper;
        this.ticketOperationLogMapper = ticketOperationLogMapper;
        this.slaPolicyMapper = slaPolicyMapper;
        this.slaAlertMapper = slaAlertMapper;
        this.slaAlertMessageProducer = slaAlertMessageProducer;
        this.assignmentRuleMapper = assignmentRuleMapper;
        this.skillGroupMapper = skillGroupMapper;
        this.skillGroupMemberMapper = skillGroupMemberMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO manualTransfer(Long sessionId, ConversationManualTransferRequest request) {
        ConversationManualTransferRequest safeRequest = request == null ? new ConversationManualTransferRequest() : request;
        ConversationSession conversationSession = conversationSessionMapper.selectById(sessionId);
        if(conversationSession == null)
            throw new BusinessException(ResultCode.SESSION_NOT_EXIST, "会话不存在");
        if("CLOSED".equals(conversationSession.getStatus()))
            throw new BusinessException(ResultCode.SESSION_CLOSED, "会话已经关闭");

        // 同一会话只能保留一张未完成工单，防止用户连续点击“转人工”造成重复待审核任务。
        Long existingCount = ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getSessionId, sessionId)
                .eq(Ticket::getDeleted, 0)
                .in(Ticket::getStatus, "PENDING_REVIEW", "WAITING_ASSIGN", "ASSIGNED", "PROCESSING")
        );
        if(existingCount != null && existingCount > 0)
            throw new BusinessException(ResultCode.CONFLICT, "工单已经存在");

        // 原始客户诉求与 AI 摘要分开存储，审核人可以对照判断 AI 是否理解正确。
        String originalContent = StringUtils.hasText(safeRequest.getOriginalContent())
                ? safeRequest.getOriginalContent()
                : findLatestCustomerMessageContent(sessionId);
        if (!StringUtils.hasText(originalContent)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户原始诉求不能为空");
        }

        Ticket ticket = new Ticket();
        LocalDateTime now = LocalDateTime.now();
        ticket.setTicketNo(generateTicketNo(now));
        ticket.setCustomerId(conversationSession.getCustomerId());
        ticket.setSessionId(sessionId);
        ticket.setTitle(defaultIfBlank(safeRequest.getTitle(), "客户转人工工单"));
        ticket.setContent(defaultIfBlank(safeRequest.getContent(), defaultIfBlank(safeRequest.getAiSummary(), originalContent)));
        ticket.setOriginalContent(originalContent);
        ticket.setAiSummary(safeRequest.getAiSummary());
        // 分类缺失时回退到 GENERAL，确保后续可匹配通用派单规则而不是落入已废弃的 UNKNOWN 分类。
        ticket.setCategory(defaultIfBlank(safeRequest.getCategory(), "GENERAL"));
        ticket.setPriority(defaultIfBlank(safeRequest.getPriority(), "P3"));
        ticket.setSuggestedAction(safeRequest.getSuggestedAction());
        ticket.setAiConfidence(safeRequest.getAiConfidence());
        ticket.setStatus("PENDING_REVIEW");
        ticket.setReviewStatus("PENDING_REVIEW");
        ticket.setSourceChannel(conversationSession.getChannel());
        ticket.setCreatedBy(SecurityUtils.getCurrentUserId());
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticket.setDeleted(0);

        if (ticketMapper.insert(ticket) == 0) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "创建转人工工单失败");
        }

        // 建单、工单操作日志和会话系统消息处于同一事务，避免出现只建单未留痕的半完成状态。
        insertTicketOperationLog(ticket.getId(), SecurityUtils.getCurrentUserId(), "CREATE", null,
                "PENDING_REVIEW", "创建转人工待审核工单", now);

        insertManualTransferSystemMessage(sessionId, safeRequest, now);

        TicketVO ticketVO = new TicketVO();
        BeanUtils.copyProperties(ticket, ticketVO);
        return ticketVO;
    }

    @Override
    public PageResult<TicketVO> pagePendingReviewTickets(TicketQueryRequest request) {
        // TODO 1. 构造 MyBatis-Plus Page<Ticket>，默认查询 reviewStatus/status 为 PENDING_REVIEW 的未删除工单。
        TicketQueryRequest safeRequest = request == null ? new TicketQueryRequest() : request;
        Page<Ticket> ticketPage = new Page<>(safeRequest.getPage(), safeRequest.getSize());
        // TODO 2. 支持 keyword、customerId、sessionId、priority、category 等筛选条件。
        // TODO 3. 按 createdAt/id 倒序排序。
        Page<Ticket> ticketList= ticketMapper.selectPage(ticketPage, new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getDeleted, 0)
                .eq(Ticket::getReviewStatus, "PENDING_REVIEW")
                .eq(Ticket::getStatus, "PENDING_REVIEW")
                .and(StringUtils.hasText(safeRequest.getKeyword()), wrapper -> wrapper
                        .like(Ticket::getTicketNo, safeRequest.getKeyword())
                        .or()
                        .like(Ticket::getTitle, safeRequest.getKeyword())
                        .or()
                        .like(Ticket::getOriginalContent, safeRequest.getKeyword())
                        .or()
                        .like(Ticket::getAiSummary, safeRequest.getKeyword()))
                .eq(safeRequest.getSessionId() != null, Ticket::getSessionId, safeRequest.getSessionId())
                .eq(safeRequest.getCustomerId() != null, Ticket::getCustomerId, safeRequest.getCustomerId())
                .eq(StringUtils.hasText(safeRequest.getCategory()), Ticket::getCategory, safeRequest.getCategory())
                .eq(StringUtils.hasText(safeRequest.getPriority()), Ticket::getPriority, safeRequest.getPriority())
                .orderByDesc(Ticket::getCreatedAt)
                .orderByDesc(Ticket::getId)
        );
        // TODO 4. 将 Ticket 转为 TicketVO，并返回 PageResult。
        List<Ticket> tickets= ticketList.getRecords();
        List<TicketVO> ticketVOS = tickets.stream().map(
               entity -> {
                   TicketVO ticketVO = new TicketVO();
                   BeanUtils.copyProperties(entity, ticketVO);
                   return ticketVO;
               }
        ).toList();
        return PageResult.of(ticketVOS, ticketList.getCurrent(), ticketList.getSize(), ticketList.getTotal());
    }

    @Override
    public TicketVO getTicketDetail(Long id) {
        // TODO 1. 根据 id 查询 ticket。
        Ticket ticket = ticketMapper.selectById(id);
        // TODO 2. 不存在或 deleted=1 时抛 TICKET_NOT_FOUND。
        if(ticket == null || Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        // TODO 3. 转换为 TicketVO 返回。
        TicketVO ticketVO = new TicketVO();
        BeanUtils.copyProperties(ticket, ticketVO);
        return ticketVO;
    }

    @Override
    public TicketFullDetailVO getTicketFullDetail(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null || Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");

        TicketVO ticketVO = new TicketVO();
        BeanUtils.copyProperties(ticket, ticketVO);

        List<TicketProcessRecordVO> processRecords = ticketProcessRecordMapper.selectList(new LambdaQueryWrapper<TicketProcessRecord>()
                .eq(TicketProcessRecord::getTicketId, id)
                .eq(TicketProcessRecord::getDeleted, 0)
                .orderByAsc(TicketProcessRecord::getCreatedAt)
                .orderByAsc(TicketProcessRecord::getId)
        ).stream().map(entity -> {
            TicketProcessRecordVO ticketProcessRecordVO = new TicketProcessRecordVO();
            BeanUtils.copyProperties(entity, ticketProcessRecordVO);
            return ticketProcessRecordVO;
        }).toList();

        List<TicketOperationLogVO> operationLogs = ticketOperationLogMapper.selectList(new LambdaQueryWrapper<TicketOperationLog>()
                .eq(TicketOperationLog::getTicketId, id)
                .eq(TicketOperationLog::getDeleted, 0)
                .orderByAsc(TicketOperationLog::getCreatedAt)
                .orderByAsc(TicketOperationLog::getId)
        ).stream().map(entity -> {
            TicketOperationLogVO ticketOperationLogVO = new TicketOperationLogVO();
            BeanUtils.copyProperties(entity, ticketOperationLogVO);
            return ticketOperationLogVO;
        }).toList();

        TicketFullDetailVO ticketFullDetailVO = new TicketFullDetailVO();
        ticketFullDetailVO.setTicket(ticketVO);
        ticketFullDetailVO.setProcessRecords(processRecords);
        ticketFullDetailVO.setOperationLogs(operationLogs);
        return ticketFullDetailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO reviewTicket(Long id, TicketReviewRequest request) {
        // TODO 1. 校验 request 不为空，reviewResult 只能是 APPROVED/REJECTED。
        if(request == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求为空");
        if(!"APPROVED".equals(request.getReviewResult()) && !"REJECTED".equals(request.getReviewResult()))
            throw new BusinessException(ResultCode.BAD_REQUEST, "审核结果只能为approved或rejected");
        // TODO 2. 查询 ticket，不存在或 deleted=1 时抛 TICKET_NOT_FOUND。
        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null || Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        // TODO 3. 只允许 reviewStatus/status 为 PENDING_REVIEW 的工单被审核。
        if(!"PENDING_REVIEW".equals(ticket.getReviewStatus()) || !"PENDING_REVIEW".equals(ticket.getStatus()))
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "工单状态不合法");
        if("REJECTED".equals(request.getReviewResult()) && !StringUtils.hasText(request.getReviewRemark()))
            throw new BusinessException(ResultCode.BAD_REQUEST, "驳回时必须填写审核备注");
        // TODO 4. 将审核员修改后的 title/content/aiSummary/category/priority/suggestedAction 写回 ticket。
        if(StringUtils.hasText(request.getTitle()))
            ticket.setTitle(request.getTitle());
        if(StringUtils.hasText(request.getContent()))
            ticket.setContent(request.getContent());
        if(StringUtils.hasText(request.getAiSummary()))
            ticket.setAiSummary(request.getAiSummary());
        if(StringUtils.hasText(request.getCategory()))
            ticket.setCategory(request.getCategory());
        if(StringUtils.hasText(request.getPriority()))
            ticket.setPriority(request.getPriority());
        if(StringUtils.hasText(request.getSuggestedAction()))
            ticket.setSuggestedAction(request.getSuggestedAction());
        if(StringUtils.hasText(request.getReviewRemark()))
            ticket.setReviewRemark(request.getReviewRemark());
        // TODO 5. 设置 reviewerId=SecurityUtils.getCurrentUserId()，reviewedAt=now，reviewRemark。
        Long reviewerId = SecurityUtils.getCurrentUserId();
        if(reviewerId == null)
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前审核人");
        LocalDateTime now = LocalDateTime.now();
        ticket.setReviewerId(reviewerId);
        ticket.setReviewedAt(now);
        ticket.setUpdatedAt(now);
        // TODO 6. APPROVED：reviewStatus=APPROVED，status=WAITING_ASSIGN。
        // TODO 7. REJECTED：reviewStatus=REJECTED，status=REJECTED 或 CLOSED。
        if("APPROVED".equals(request.getReviewResult())) {
            ticket.setReviewStatus(request.getReviewResult());
            ticket.setStatus("WAITING_ASSIGN");
        }
        else {
            ticket.setReviewStatus(request.getReviewResult());
            ticket.setStatus("REJECTED");
        }
        // TODO 8. 更新数据库，并返回最新 TicketVO。
        // 将旧状态放入更新条件，实现轻量乐观锁；审核人并发操作时只有第一个请求能改变状态。
        int flag = ticketMapper.update(ticket, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getReviewStatus, "PENDING_REVIEW")
                .eq(Ticket::getStatus, "PENDING_REVIEW")
                .eq(Ticket::getDeleted, 0));
        if(flag != 1)
            throw new BusinessException(ResultCode.CONFLICT, "工单状态已变化，审核失败");

        String operationType = "APPROVED".equals(request.getReviewResult()) ? "REVIEW_APPROVE" : "REVIEW_REJECT";
        String operationContent = "APPROVED".equals(request.getReviewResult()) ? "审核通过工单" : "驳回工单";
        if(StringUtils.hasText(request.getReviewRemark())) {
            operationContent = operationContent + "，备注：" + request.getReviewRemark();
        }
        insertTicketOperationLog(ticket.getId(), reviewerId, operationType, "PENDING_REVIEW",
                ticket.getStatus(), operationContent, now);

        // 审核事务提交后再尝试自动派单，失败时工单保留 WAITING_ASSIGN。
        if ("APPROVED".equals(request.getReviewResult())) {
            applicationEventPublisher.publishEvent(new TicketApprovedEvent(ticket.getId()));
        }

        TicketVO ticketVO = new TicketVO();
        BeanUtils.copyProperties(ticket, ticketVO);
        return ticketVO;
    }

    @Override
    public PageResult<TicketVO> pageWaitingAssignTickets(TicketQueryRequest request) {
        TicketQueryRequest safeRequest = request == null ? new TicketQueryRequest() : request;
        Page<Ticket> ticketPage = new Page<>(safeRequest.getPage(), safeRequest.getSize());
        Page<Ticket> ticketList = ticketMapper.selectPage(ticketPage, new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getDeleted, 0)
                .eq(Ticket::getReviewStatus, "APPROVED")
                .eq(Ticket::getStatus, "WAITING_ASSIGN")
                .and(StringUtils.hasText(safeRequest.getKeyword()), wrapper -> appendKeywordCondition(wrapper, safeRequest.getKeyword()))
                .eq(safeRequest.getSessionId() != null, Ticket::getSessionId, safeRequest.getSessionId())
                .eq(safeRequest.getCustomerId() != null, Ticket::getCustomerId, safeRequest.getCustomerId())
                .eq(StringUtils.hasText(safeRequest.getCategory()), Ticket::getCategory, safeRequest.getCategory())
                .eq(StringUtils.hasText(safeRequest.getPriority()), Ticket::getPriority, safeRequest.getPriority())
                .orderByDesc(Ticket::getCreatedAt)
                .orderByDesc(Ticket::getId)
        );
        return toPageResult(ticketList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO assignTicket(Long id, TicketAssignRequest request) {
        if(request == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求为空");
        if(request.getAssigneeId() == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "处理客服ID不能为空");
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if(currentUserId == null)
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前用户");

        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null || Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        if(!"APPROVED".equals(ticket.getReviewStatus()) || !"WAITING_ASSIGN".equals(ticket.getStatus()))
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "只有审核通过且待派单的工单可以派单");

        LocalDateTime now = LocalDateTime.now();
        ticket.setAssigneeId(request.getAssigneeId());
        ticket.setSkillGroupId(request.getSkillGroupId());
        ticket.setStatus("ASSIGNED");
        ticket.setUpdatedAt(now);

        int updated = ticketMapper.update(ticket, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getReviewStatus, "APPROVED")
                .eq(Ticket::getStatus, "WAITING_ASSIGN")
                .eq(Ticket::getDeleted, 0));
        if(updated != 1)
            throw new BusinessException(ResultCode.CONFLICT, "工单状态已变化，派单失败");

        syncConversationAfterAssign(ticket, request.getAssigneeId(), now);

        // 派单成功后立即应用 SLA，保证客服接手的工单一定具有截止时间和自动告警计划。
        applySlaToTicket(ticket, now, currentUserId);

        // TODO: 派单成功后，为 request.assigneeId 创建一条 TICKET_ASSIGNED 未读站内通知。
        Notification notification = new Notification();
        notification.setDeleted(0);
        notification.setReceiverId(ticket.getAssigneeId());
        notification.setType("TICKET_ASSIGNED");
        notification.setTitle("您有新的待处理工单");
        notification.setContent("工单 " + ticket.getTicketNo()
                + "（" + defaultIfBlank(ticket.getPriority(), "P3") + "）："
                + defaultIfBlank(ticket.getTitle(), "客户咨询工单")
                + "，来源 " + defaultIfBlank(ticket.getSourceChannel(), "UNKNOWN") + "，请及时处理。");
        notification.setBusinessType("TICKET");
        notification.setBusinessId(ticket.getId());
        notification.setReadStatus("UNREAD");
        notification.setCreatedAt(now);
        int insert = notificationMapper.insert(notification);
        if(insert != 1)
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "通知插入失败");

        String assignContent = "派单给客服：" + request.getAssigneeId();
        if(request.getSkillGroupId() != null) {
            assignContent = assignContent + "，技能组：" + request.getSkillGroupId();
        }
        if(StringUtils.hasText(request.getAssignRemark())) {
            assignContent = assignContent + "，备注：" + request.getAssignRemark();
        }
        insertTicketOperationLog(ticket.getId(), currentUserId, "ASSIGN",
                "WAITING_ASSIGN", "ASSIGNED", assignContent, now);
        TicketVO ticketVO = new TicketVO();
        BeanUtils.copyProperties(ticket, ticketVO);
        return ticketVO;
    }

    @Override
    public TicketAutoAssignPreviewVO previewAutoAssign(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null || Integer.valueOf(1).equals(ticket.getDeleted())) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        }
        if (!"APPROVED".equals(ticket.getReviewStatus()) || !"WAITING_ASSIGN".equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "当前工单不允许自动派单");
        }

        TicketAutoAssignPreviewVO previewVO = new TicketAutoAssignPreviewVO();
        previewVO.setTicketId(ticket.getId());
        previewVO.setTicketNo(ticket.getTicketNo());
        previewVO.setCategory(ticket.getCategory());
        previewVO.setPriority(ticket.getPriority());

        AssignmentRule rule = assignmentRuleMapper.selectOne(new LambdaQueryWrapper<AssignmentRule>()
                .eq(AssignmentRule::getCategory, ticket.getCategory())
                .eq(AssignmentRule::getPriority, ticket.getPriority())
                .eq(AssignmentRule::getEnabled, 1)
                .orderByDesc(AssignmentRule::getRuleWeight)
                .orderByAsc(AssignmentRule::getId)
                .last("LIMIT 1"));
        if (rule == null) {
            previewVO.setCandidates(List.of());
            previewVO.setUnavailableReason("未找到匹配的启用派单规则");
            return previewVO;
        }

        previewVO.setAssignmentRuleId(rule.getId());
        previewVO.setAssignmentRuleName(rule.getRuleName());
        previewVO.setRuleWeight(rule.getRuleWeight());

        SkillGroup skillGroup = skillGroupMapper.selectById(rule.getSkillGroupId());
        if (skillGroup == null || !Integer.valueOf(1).equals(skillGroup.getStatus())) {
            previewVO.setCandidates(List.of());
            previewVO.setUnavailableReason("匹配的技能组不存在或已停用");
            return previewVO;
        }

        previewVO.setSkillGroupId(skillGroup.getId());
        previewVO.setSkillGroupName(skillGroup.getGroupName());

        // 候选人已按当前处理中工单数升序排序，并排除了停用和已满载客服。
        List<SkillGroupAgentLoadVO> candidates =
                skillGroupMemberMapper.selectAvailableAgentsWithLoad(skillGroup.getId());
        previewVO.setCandidates(candidates);
        if (candidates.isEmpty()) {
            previewVO.setUnavailableReason("技能组内没有可分配的客服");
            return previewVO;
        }

        SkillGroupAgentLoadVO selectedAgent = candidates.get(0);
        previewVO.setSelectedAgentId(selectedAgent.getUserId());
        previewVO.setSelectedAgentName(StringUtils.hasText(selectedAgent.getRealName())
                ? selectedAgent.getRealName()
                : selectedAgent.getUsername());
        return previewVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO autoAssignTicket(Long id) {
        TicketAutoAssignPreviewVO previewVO = previewAutoAssign(id);
        if (previewVO.getSkillGroupId() == null) {
            throw new BusinessException(
                    ResultCode.CONFLICT,
                    previewVO.getUnavailableReason()
            );
        }

        // 锁住技能组后重新计算候选人负载，避免并发请求同时选中同一位客服。
        SkillGroup lockedSkillGroup = skillGroupMapper.selectActiveByIdForUpdate(previewVO.getSkillGroupId());
        if (lockedSkillGroup == null) {
            throw new BusinessException(ResultCode.CONFLICT, "技能组不存在或已停用");
        }

        List<SkillGroupAgentLoadVO> candidates =
                skillGroupMemberMapper.selectAvailableAgentsWithLoad(lockedSkillGroup.getId());
        if (candidates.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "技能组内没有可分配的客服");
        }

        SkillGroupAgentLoadVO selectedAgent = candidates.get(0);
        TicketAssignRequest assignRequest = new TicketAssignRequest();
        assignRequest.setAssigneeId(selectedAgent.getUserId());
        assignRequest.setSkillGroupId(lockedSkillGroup.getId());
        assignRequest.setAssignRemark(
                "系统自动派单，命中规则：" + previewVO.getAssignmentRuleName()
        );

        // TODO: 自动派单复用 assignTicket，由其中统一创建派单通知，避免重复发送。
        // 复用派单主流程，同步处理会话、SLA 告警和操作日志。
        return assignTicket(id, assignRequest);
    }

    @Override
    public PageResult<TicketVO> pageMyTickets(TicketQueryRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if(currentUserId == null)
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前用户");

        TicketQueryRequest safeRequest = request == null ? new TicketQueryRequest() : request;
        Page<Ticket> ticketPage = new Page<>(safeRequest.getPage(), safeRequest.getSize());
        Page<Ticket> ticketList = ticketMapper.selectPage(ticketPage, new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getDeleted, 0)
                .eq(Ticket::getAssigneeId, currentUserId)
                .in(Ticket::getStatus, "ASSIGNED", "PROCESSING")
                .and(StringUtils.hasText(safeRequest.getKeyword()), wrapper -> appendKeywordCondition(wrapper, safeRequest.getKeyword()))
                .eq(safeRequest.getSessionId() != null, Ticket::getSessionId, safeRequest.getSessionId())
                .eq(safeRequest.getCustomerId() != null, Ticket::getCustomerId, safeRequest.getCustomerId())
                .eq(StringUtils.hasText(safeRequest.getCategory()), Ticket::getCategory, safeRequest.getCategory())
                .eq(StringUtils.hasText(safeRequest.getPriority()), Ticket::getPriority, safeRequest.getPriority())
                .orderByDesc(Ticket::getUpdatedAt)
                .orderByDesc(Ticket::getId)
        );
        return toPageResult(ticketList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO startTicket(Long id, TicketStartRequest request) {
        // TODO 1. 查询 ticket，校验存在且 deleted=0。
        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "工单不存在");
        if(Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        // TODO 2. 校验当前登录用户是该工单 assigneeId。
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if(currentUserId == null)
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前用户");
        if(ticket.getAssigneeId() == null || !Objects.equals(currentUserId, ticket.getAssigneeId()))
            throw new BusinessException(ResultCode.FORBIDDEN, "只能处理分配给自己的工单");
        // TODO 3. 只允许 ASSIGNED 状态进入 PROCESSING。
        if(!"ASSIGNED".equals(ticket.getStatus()))
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "只有已分配的工单可以开始处理");
        // TODO 4. 使用 LambdaUpdateWrapper 做状态条件更新，防止并发重复开始处理。
        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus("PROCESSING");
        ticket.setUpdatedAt(now);
        // 同时约束处理人与旧状态，避免已转派或已开始处理的工单被旧请求再次启动。
        int updated = ticketMapper.update(ticket, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, "ASSIGNED")
                .eq(Ticket::getAssigneeId, currentUserId)
                .eq(Ticket::getDeleted, 0)
        );
        if(updated != 1)
            throw new BusinessException(ResultCode.CONFLICT, "工单状态已变化，开始处理失败");
        // TODO 5. 插入 ticket_process_record，recordType 建议为 START。
        TicketProcessRecord ticketProcessRecord = new TicketProcessRecord();
        ticketProcessRecord.setTicketId(ticket.getId());
        String recordContent = request != null && StringUtils.hasText(request.getRemark())
                ? request.getRemark()
                : "客服开始处理工单";
        ticketProcessRecord.setContent(recordContent);
        ticketProcessRecord.setDeleted(0);
        ticketProcessRecord.setCreatedAt(now);
        ticketProcessRecord.setRecordType("START");
        ticketProcessRecord.setOperatorId(currentUserId);
        ticketProcessRecord.setVisibleToCustomer(0);
        ticketProcessRecordMapper.insert(ticketProcessRecord);
        insertTicketOperationLog(ticket.getId(), currentUserId, "START", "ASSIGNED",
                "PROCESSING", recordContent, now);
        // TODO 6. 返回最新 TicketVO。
        TicketVO ticketVO = new TicketVO();
        BeanUtils.copyProperties(ticket, ticketVO);
        return ticketVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketProcessRecordVO addTicketProcessRecord(Long id, TicketProcessRecordCreateRequest request) {
        // TODO 1. 查询 ticket，校验存在且 deleted=0。
        if(request == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求为空");
        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "工单不存在");
        if(Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        // TODO 2. 校验当前登录用户是该工单 assigneeId，或拥有主管/管理员处理权限。
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if(currentUserId == null)
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前用户");
        boolean manager = SecurityUtils.getCurrentUser() != null
                && SecurityUtils.getCurrentUser().getRoleCodes() != null
                && (SecurityUtils.getCurrentUser().getRoleCodes().contains("ADMIN")
                || SecurityUtils.getCurrentUser().getRoleCodes().contains("SUPERVISOR"));
        if(!manager && (ticket.getAssigneeId() == null || !Objects.equals(currentUserId, ticket.getAssigneeId())))
            throw new BusinessException(ResultCode.FORBIDDEN, "只能给自己负责的工单添加处理记录");
        // TODO 3. 只允许 ASSIGNED/PROCESSING 状态添加处理记录。
        if(!"ASSIGNED".equals(ticket.getStatus()) && !"PROCESSING".equals(ticket.getStatus()))
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "当前工单状态不允许");
        // TODO 4. 插入 ticket_process_record，recordType 默认 NOTE，visibleToCustomer 默认 0。
        LocalDateTime now = LocalDateTime.now();
        TicketProcessRecord ticketProcessRecord = new TicketProcessRecord();
        ticketProcessRecord.setTicketId(ticket.getId());
        ticketProcessRecord.setOperatorId(currentUserId);
        ticketProcessRecord.setRecordType(StringUtils.hasText(request.getRecordType())? request.getRecordType() : "NOTE");
        ticketProcessRecord.setContent(request.getContent());
        ticketProcessRecord.setVisibleToCustomer(request.getVisibleToCustomer() == null ? 0 : request.getVisibleToCustomer());
        ticketProcessRecord.setCreatedAt(now);
        ticketProcessRecord.setDeleted(0);
        int index = ticketProcessRecordMapper.insert(ticketProcessRecord);
        if(index == 0)
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "数据库插入失败");
        // TODO 5. 更新 ticket.updatedAt。
        ticketMapper.update(null, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getDeleted, 0)
                .set(Ticket::getUpdatedAt, now));
        insertTicketOperationLog(ticket.getId(), currentUserId, "ADD_RECORD", ticket.getStatus(),
                ticket.getStatus(), "新增工单处理记录：" + ticketProcessRecord.getRecordType(), now);
        // TODO 6. 返回 TicketProcessRecordVO。
        TicketProcessRecordVO ticketProcessRecordVO = new TicketProcessRecordVO();
        BeanUtils.copyProperties(ticketProcessRecord, ticketProcessRecordVO);
        return ticketProcessRecordVO;
    }

    @Override
    public PageResult<TicketProcessRecordVO> pageTicketProcessRecords(Long id, TicketProcessRecordQueryRequest request) {
        // TODO 1. 查询 ticket，校验存在且 deleted=0。
        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null || Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        // TODO 2. 构造 Page<TicketProcessRecord>。
        TicketProcessRecordQueryRequest safeRequest = request == null ? new TicketProcessRecordQueryRequest() : request;
        Page<TicketProcessRecord> recordPage = new Page<>(safeRequest.getPage(), safeRequest.getSize());
        // TODO 3. 按 ticketId、deleted=0 查询，可选 recordType、visibleToCustomer 筛选。
        Page<TicketProcessRecord> pageResult = ticketProcessRecordMapper.selectPage(recordPage, new LambdaQueryWrapper<TicketProcessRecord>()
                .eq(TicketProcessRecord::getTicketId, id)
                .eq(TicketProcessRecord::getDeleted, 0)
                .eq(StringUtils.hasText(safeRequest.getRecordType()), TicketProcessRecord::getRecordType, safeRequest.getRecordType())
                .eq(safeRequest.getVisibleToCustomer() != null, TicketProcessRecord::getVisibleToCustomer, safeRequest.getVisibleToCustomer())
                .orderByAsc(TicketProcessRecord::getCreatedAt)
                .orderByAsc(TicketProcessRecord::getId)
        );
        // TODO 4. 按 createdAt/id 倒序或正序排序。做时间线展示时推荐正序。
        // TODO 5. 转换为 PageResult<TicketProcessRecordVO> 返回。
        List<TicketProcessRecordVO> records = pageResult.getRecords().stream().map(entity -> {
            TicketProcessRecordVO ticketProcessRecordVO = new TicketProcessRecordVO();
            BeanUtils.copyProperties(entity, ticketProcessRecordVO);
            return ticketProcessRecordVO;
        }).toList();
        return PageResult.of(records, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    @Override
    public PageResult<TicketOperationLogVO> pageTicketOperationLogs(Long id, TicketOperationLogQueryRequest request) {
        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null || Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");

        TicketOperationLogQueryRequest safeRequest = request == null ? new TicketOperationLogQueryRequest() : request;
        Page<TicketOperationLog> logPage = new Page<>(safeRequest.getPage(), safeRequest.getSize());
        Page<TicketOperationLog> pageResult = ticketOperationLogMapper.selectPage(logPage, new LambdaQueryWrapper<TicketOperationLog>()
                .eq(TicketOperationLog::getTicketId, id)
                .eq(TicketOperationLog::getDeleted, 0)
                .eq(StringUtils.hasText(safeRequest.getOperationType()), TicketOperationLog::getOperationType, safeRequest.getOperationType())
                .orderByAsc(TicketOperationLog::getCreatedAt)
                .orderByAsc(TicketOperationLog::getId)
        );

        List<TicketOperationLogVO> records = pageResult.getRecords().stream().map(entity -> {
            TicketOperationLogVO ticketOperationLogVO = new TicketOperationLogVO();
            BeanUtils.copyProperties(entity, ticketOperationLogVO);
            return ticketOperationLogVO;
        }).toList();
        return PageResult.of(records, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO resolveTicket(Long id, TicketResolveRequest request) {
        // TODO 1. 查询 ticket，校验存在且 deleted=0。
        if(request == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求为空");
        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null || Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        // TODO 2. 校验当前登录用户是该工单 assigneeId。
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if(currentUserId == null)
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前用户");
        if(ticket.getAssigneeId() == null || !Objects.equals(currentUserId, ticket.getAssigneeId()))
            throw new BusinessException(ResultCode.FORBIDDEN, "只能解决自己负责的工单");
        // 会话中的客服可能尚未显式点击“开始处理”，因此已分配或处理中均可确认解决。
        if(!List.of("ASSIGNED", "PROCESSING").contains(ticket.getStatus()))
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "当前状态不允许解决工单");
        // TODO 4. 将 ticket.status 更新为 RESOLVED，并更新 updatedAt。
        LocalDateTime now = LocalDateTime.now();
        String oldStatus = ticket.getStatus();
        ticket.setUpdatedAt(now);
        ticket.setStatus("RESOLVED");
        int flag = ticketMapper.update(ticket, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, oldStatus)
                .eq(Ticket::getAssigneeId, currentUserId)
                .eq(Ticket::getDeleted, 0));
        if(flag != 1)
            throw new BusinessException(ResultCode.CONFLICT, "工单状态已变化，解决失败");
        // TODO 5. 插入 ticket_process_record，recordType 建议为 RESOLVE，content 使用 request.solution。
        TicketProcessRecord ticketProcessRecord = new TicketProcessRecord();
        ticketProcessRecord.setContent(request.getSolution());
        ticketProcessRecord.setDeleted(0);
        ticketProcessRecord.setTicketId(ticket.getId());
        ticketProcessRecord.setRecordType("RESOLVE");
        ticketProcessRecord.setOperatorId(currentUserId);
        ticketProcessRecord.setVisibleToCustomer(request.getVisibleToCustomer() == null ? 1 : request.getVisibleToCustomer());
        ticketProcessRecord.setCreatedAt(now);
        int inserted = ticketProcessRecordMapper.insert(ticketProcessRecord);
        if(inserted != 1)
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "处理记录插入失败");
        insertTicketOperationLog(ticket.getId(), currentUserId, "RESOLVE", oldStatus,
                "RESOLVED", "解决工单：" + request.getSolution(), now);
        TicketVO ticketVO = new TicketVO();
        BeanUtils.copyProperties(ticket, ticketVO);
        // TODO 6. 返回最新 TicketVO。
        return ticketVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO closeTicket(Long id, TicketCloseRequest request) {
        // TODO 1. 查询 ticket，校验存在且 deleted=0。
        if(request == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求为空");
        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null || Integer.valueOf(1).equals(ticket.getDeleted()))
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        // TODO 2. 推荐普通客服只允许关闭自己且 RESOLVED 状态的工单；ADMIN/SUPERVISOR 可考虑强制关闭。
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if(currentUserId == null)
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前用户");
        boolean manager = isManager();
        if(!manager && (ticket.getAssigneeId() == null || !Objects.equals(currentUserId, ticket.getAssigneeId())))
            throw new BusinessException(ResultCode.FORBIDDEN, "只能关闭自己负责的工单");
        if(!manager && !"RESOLVED".equals(ticket.getStatus()))
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "只有已解决的工单可以关闭");
        if(manager && !List.of("ASSIGNED", "PROCESSING", "RESOLVED").contains(ticket.getStatus()))
            throw new BusinessException(ResultCode.TICKET_STATUS_INVALID, "当前工单状态不允许关闭");
        // TODO 3. 将 ticket.status 更新为 CLOSED，设置 closedAt 和 updatedAt。
        LocalDateTime now = LocalDateTime.now();
        String oldStatus = ticket.getStatus();
        ticket.setStatus("CLOSED");
        ticket.setClosedAt(now);
        ticket.setUpdatedAt(now);
        LambdaUpdateWrapper<Ticket> updateWrapper = new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, oldStatus)
                .eq(Ticket::getDeleted, 0)
                .set(Ticket::getStatus, "CLOSED")
                .set(Ticket::getClosedAt, now)
                .set(Ticket::getUpdatedAt, now);
        if(!manager) {
            updateWrapper.eq(Ticket::getAssigneeId, currentUserId);
        }
        int updated = ticketMapper.update(null, updateWrapper);
        if(updated != 1)
            throw new BusinessException(ResultCode.CONFLICT, "工单状态已变化，关闭失败");
        // TODO 4. 插入 ticket_process_record，recordType 建议为 CLOSE，content 使用 request.closeReason。
        TicketProcessRecord ticketProcessRecord = new TicketProcessRecord();
        ticketProcessRecord.setTicketId(ticket.getId());
        ticketProcessRecord.setOperatorId(currentUserId);
        ticketProcessRecord.setRecordType("CLOSE");
        ticketProcessRecord.setContent(request.getCloseReason());
        ticketProcessRecord.setVisibleToCustomer(0);
        ticketProcessRecord.setCreatedAt(now);
        ticketProcessRecord.setDeleted(0);
        int inserted = ticketProcessRecordMapper.insert(ticketProcessRecord);
        if(inserted != 1)
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "处理记录插入失败");
        // TODO 5. 如果工单关联会话，可按业务需要同步关闭 conversation_session。
        syncConversationAfterClose(ticket, request.getCloseReason(), now);
        insertTicketOperationLog(ticket.getId(), currentUserId, "CLOSE", oldStatus,
                "CLOSED", "关闭工单，原因：" + request.getCloseReason(), now);
        // TODO 6. 返回最新 TicketVO。
        TicketVO ticketVO = new TicketVO();
        BeanUtils.copyProperties(ticket, ticketVO);
        return ticketVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketApplySlaVO applySla(Long id, TicketApplySlaRequest request) {
        if(request == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求参数为空");
        // TODO 1. 查询 ticket，校验存在且 deleted=0，且状态不是 CLOSED/REJECTED。
        Ticket ticket = ticketMapper.selectById(id);
        if(ticket == null)
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "工单不存在");
        if(Integer.valueOf(1).equals(ticket.getDeleted()) || "CLOSED".equals(ticket.getStatus()) || "REJECTED".equals(ticket.getStatus()))
            throw new BusinessException(ResultCode.CONFLICT, "工单已经关闭");
        // TODO 2. 如果 overwrite != true，且工单已存在 firstResponseDeadline 或 resolveDeadline，则提示冲突。
        if(!Boolean.TRUE.equals(request.getOverwrite()))
        {
            if(ticket.getFirstRespondedAt() != null || ticket.getFirstResponseDeadline() != null)
                    throw new BusinessException(ResultCode.CONFLICT, "不允许覆写");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if(currentUserId == null)
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前用户");

        LocalDateTime baseTime = request.getBaseTime() != null
                ? request.getBaseTime()
                : LocalDateTime.now();
        return applySlaToTicket(ticket, baseTime, currentUserId);
    }

    private TicketApplySlaVO applySlaToTicket(Ticket ticket,
                                              LocalDateTime baseTime,
                                              Long operatorId) {
        // 优先匹配“分类 + 优先级”专属策略，再回退到同优先级的默认策略（category 为 null）。
        SlaPolicy slaPolicy = slaPolicyMapper.selectOne(new LambdaQueryWrapper<SlaPolicy>()
                .eq(SlaPolicy::getPriority, ticket.getPriority())
                .eq(SlaPolicy::getCategory, ticket.getCategory())
                .eq(SlaPolicy::getDeleted, 0)
                .eq(SlaPolicy::getEnabled, 1));
        // TODO 3. 先按 ticket.category + ticket.priority 查启用且未删除的 SLA 策略。
        // TODO 4. 如果没有完全匹配，再按 category IS NULL + ticket.priority 查默认 SLA 策略。
        if(slaPolicy == null)
        {
            slaPolicy = slaPolicyMapper.selectOne(new LambdaQueryWrapper<SlaPolicy>()
                    .eq(SlaPolicy::getPriority, ticket.getPriority())
                    .isNull(SlaPolicy::getCategory)
                    .eq(SlaPolicy::getDeleted, 0)
                    .eq(SlaPolicy::getEnabled, 1)
            );
        }
        if(slaPolicy == null)
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到匹配的SLA策略");

        LocalDateTime firstResponseDeadline = baseTime.plusMinutes(slaPolicy.getFirstResponseMinutes());
        LocalDateTime resolveDeadline = baseTime.plusMinutes(slaPolicy.getResolveMinutes());
        // TODO 6. 更新 ticket.firstResponseDeadline、ticket.resolveDeadline、updatedAt。
        ticket.setFirstResponseDeadline(firstResponseDeadline);
        ticket.setResolveDeadline(resolveDeadline);
        LocalDateTime now = LocalDateTime.now();
        ticket.setUpdatedAt(now);
        if (ticketMapper.updateById(ticket) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "工单状态已变化，应用SLA失败");
        }
        // 一次应用 SLA 同时创建首响/解决的风险与超时四类告警记录，
        // 告警记录是消息消费幂等和失败补偿的数据库依据。
        List<SlaAlert> slaAlerts = createSlaAlerts(ticket.getId(), firstResponseDeadline, resolveDeadline, now);
        for (SlaAlert slaAlert : slaAlerts) {
            if (slaAlertMapper.insert(slaAlert) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "SLA自动告警创建失败");
            }
        }
        publishSlaAlertsAfterCommit(slaAlerts);
        // 记录 SLA 来源，手动应用和派单自动应用共用该日志。
        insertTicketOperationLog(ticket.getId(), operatorId, "APPLY_SLA", ticket.getStatus(),
                ticket.getStatus(), "应用SLA策略：" + slaPolicy.getPolicyName(), ticket.getUpdatedAt());
        TicketApplySlaVO ticketApplySlaVO = new TicketApplySlaVO();
        ticketApplySlaVO.setTicketId(ticket.getId());
        ticketApplySlaVO.setTicketNo(ticket.getTicketNo());
        ticketApplySlaVO.setSlaPolicyId(slaPolicy.getId());
        ticketApplySlaVO.setPolicyName(slaPolicy.getPolicyName());
        ticketApplySlaVO.setCategory(slaPolicy.getCategory());
        ticketApplySlaVO.setPriority(slaPolicy.getPriority());
        ticketApplySlaVO.setFirstResponseMinutes(slaPolicy.getFirstResponseMinutes());
        ticketApplySlaVO.setResolveMinutes(slaPolicy.getResolveMinutes());
        ticketApplySlaVO.setBaseTime(baseTime);
        ticketApplySlaVO.setFirstResponseDeadline(firstResponseDeadline);
        ticketApplySlaVO.setResolveDeadline(resolveDeadline);
        return ticketApplySlaVO;
    }

    private List<SlaAlert> createSlaAlerts(Long ticketId,
                                           LocalDateTime firstResponseDeadline,
                                           LocalDateTime resolveDeadline,
                                           LocalDateTime createdAt) {
        return List.of(
                createSlaAlert(ticketId, "FIRST_RESPONSE", "RISK", firstResponseDeadline.minusMinutes(30), firstResponseDeadline, createdAt),
                createSlaAlert(ticketId, "FIRST_RESPONSE", "OVERDUE", firstResponseDeadline, firstResponseDeadline, createdAt),
                createSlaAlert(ticketId, "RESOLVE", "RISK", resolveDeadline.minusMinutes(30), resolveDeadline, createdAt),
                createSlaAlert(ticketId, "RESOLVE", "OVERDUE", resolveDeadline, resolveDeadline, createdAt)
        );
    }

    private SlaAlert createSlaAlert(Long ticketId,
                                    String alertType,
                                    String alertLevel,
                                    LocalDateTime scheduledAt,
                                    LocalDateTime deadlineAt,
                                    LocalDateTime createdAt) {
        SlaAlert slaAlert = new SlaAlert();
        slaAlert.setTicketId(ticketId);
        slaAlert.setAlertType(alertType);
        slaAlert.setAlertLevel(alertLevel);
        slaAlert.setStatus("PENDING");
        slaAlert.setScheduledAt(scheduledAt);
        slaAlert.setDeadlineAt(deadlineAt);
        slaAlert.setRetryCount(0);
        slaAlert.setCreatedAt(createdAt);
        slaAlert.setUpdatedAt(createdAt);
        return slaAlert;
    }

    private void publishSlaAlertsAfterCommit(List<SlaAlert> slaAlerts) {
        // 必须等待事务提交后再投递 RabbitMQ。否则消费者可能先收到 alertId，
        // 却因当前事务尚未提交而查询不到 sla_alert 记录。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (SlaAlert slaAlert : slaAlerts) {
                    publishSlaAlert(slaAlert);
                }
            }
        });
    }

    private void publishSlaAlert(SlaAlert slaAlert) {
        try {
            long delayMillis = Math.max(0L,
                    Duration.between(LocalDateTime.now(), slaAlert.getScheduledAt()).toMillis());
            slaAlertMessageProducer.sendDelayedAlert(new SlaAlertMessage(slaAlert.getId()), delayMillis);
        } catch (RuntimeException exception) {
            // 提交后发送失败无法回滚主事务，因此将告警标为 FAILED，交由后续重试接口补偿。
            slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                    .eq(SlaAlert::getId, slaAlert.getId())
                    .eq(SlaAlert::getStatus, "PENDING")
                    .set(SlaAlert::getStatus, "FAILED")
                    .set(SlaAlert::getLastErrorMessage, truncateAlertError("延迟消息投递异常：" + exception.getMessage()))
                    .set(SlaAlert::getUpdatedAt, LocalDateTime.now()));
        }
    }

    private String truncateAlertError(String message) {
        if (message == null) {
            return "延迟消息投递异常";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private String findLatestCustomerMessageContent(Long sessionId) {
        ConversationMessage message = conversationMessageMapper.selectOne(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getSessionId, sessionId)
                        .eq(ConversationMessage::getSenderType, "CUSTOMER")
                        .orderByDesc(ConversationMessage::getCreatedAt)
                        .orderByDesc(ConversationMessage::getId)
                        .last("LIMIT 1")
        );
        return message == null ? null : message.getContent();
    }

    private void insertManualTransferSystemMessage(Long sessionId,
                                                   ConversationManualTransferRequest request,
                                                   LocalDateTime createdAt) {
        ConversationMessage systemMessage = new ConversationMessage();
        systemMessage.setSessionId(sessionId);
        systemMessage.setSenderType("SYSTEM");
        systemMessage.setMessageType("SYSTEM");
        String reason = StringUtils.hasText(request.getTransferReason())
                ? "，原因：" + request.getTransferReason()
                : "";
        systemMessage.setContent("用户已申请转人工，系统已创建待审核工单" + reason);
        systemMessage.setCreatedAt(createdAt);
        conversationMessageMapper.insert(systemMessage);
    }

    private String generateTicketNo(LocalDateTime now) {
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "T" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + random;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private LambdaQueryWrapper<Ticket> appendKeywordCondition(LambdaQueryWrapper<Ticket> wrapper, String keyword) {
        return wrapper.like(Ticket::getTicketNo, keyword)
                .or()
                .like(Ticket::getTitle, keyword)
                .or()
                .like(Ticket::getOriginalContent, keyword)
                .or()
                .like(Ticket::getAiSummary, keyword);
    }

    private PageResult<TicketVO> toPageResult(Page<Ticket> ticketPage) {
        List<TicketVO> ticketVOS = ticketPage.getRecords().stream().map(entity -> {
            TicketVO ticketVO = new TicketVO();
            BeanUtils.copyProperties(entity, ticketVO);
            return ticketVO;
        }).toList();
        return PageResult.of(ticketVOS, ticketPage.getCurrent(), ticketPage.getSize(), ticketPage.getTotal());
    }

    private boolean isManager() {
        return SecurityUtils.getCurrentUser() != null
                && SecurityUtils.getCurrentUser().getRoleCodes() != null
                && (SecurityUtils.getCurrentUser().getRoleCodes().contains("ADMIN")
                || SecurityUtils.getCurrentUser().getRoleCodes().contains("SUPERVISOR"));
    }

    private void syncConversationAfterAssign(Ticket ticket, Long assigneeId, LocalDateTime now) {
        if(ticket.getSessionId() == null) {
            return;
        }

        conversationSessionMapper.update(null, new LambdaUpdateWrapper<ConversationSession>()
                .eq(ConversationSession::getId, ticket.getSessionId())
                .ne(ConversationSession::getStatus, "CLOSED")
                .set(ConversationSession::getCurrentAgentId, assigneeId)
                .set(ConversationSession::getStatus, "TAKEN_OVER")
                .set(ConversationSession::getLastMessageAt, now)
                .set(ConversationSession::getUpdatedAt, now));

        ConversationMessage systemMessage = new ConversationMessage();
        systemMessage.setSessionId(ticket.getSessionId());
        systemMessage.setSenderType("SYSTEM");
        systemMessage.setMessageType("SYSTEM");
        systemMessage.setContent("工单 " + ticket.getTicketNo() + " 已分配给客服 " + assigneeId);
        systemMessage.setCreatedAt(now);
        conversationMessageMapper.insert(systemMessage);
    }

    private void syncConversationAfterClose(Ticket ticket, String closeReason, LocalDateTime now) {
        if(ticket.getSessionId() == null) {
            return;
        }

        conversationSessionMapper.update(null, new LambdaUpdateWrapper<ConversationSession>()
                .eq(ConversationSession::getId, ticket.getSessionId())
                .ne(ConversationSession::getStatus, "CLOSED")
                .set(ConversationSession::getStatus, "CLOSED")
                .set(ConversationSession::getCurrentAgentId, null)
                .set(ConversationSession::getLastMessageAt, now)
                .set(ConversationSession::getUpdatedAt, now));

        ConversationMessage systemMessage = new ConversationMessage();
        systemMessage.setSessionId(ticket.getSessionId());
        systemMessage.setSenderType("SYSTEM");
        systemMessage.setMessageType("SYSTEM");
        systemMessage.setContent("工单 " + ticket.getTicketNo() + " 已关闭，原因：" + closeReason);
        systemMessage.setCreatedAt(now);
        conversationMessageMapper.insert(systemMessage);
    }

    private void insertTicketOperationLog(Long ticketId,
                                          Long operatorId,
                                          String operationType,
                                          String fromStatus,
                                          String toStatus,
                                          String operationContent,
                                          LocalDateTime createdAt) {
        TicketOperationLog ticketOperationLog = new TicketOperationLog();
        ticketOperationLog.setTicketId(ticketId);
        ticketOperationLog.setOperatorId(operatorId);
        ticketOperationLog.setOperationType(operationType);
        ticketOperationLog.setFromStatus(fromStatus);
        ticketOperationLog.setToStatus(toStatus);
        ticketOperationLog.setOperationContent(operationContent);
        ticketOperationLog.setCreatedAt(createdAt);
        ticketOperationLog.setDeleted(0);
        int inserted = ticketOperationLogMapper.insert(ticketOperationLog);
        if(inserted != 1)
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "工单操作日志插入失败");
    }

}
