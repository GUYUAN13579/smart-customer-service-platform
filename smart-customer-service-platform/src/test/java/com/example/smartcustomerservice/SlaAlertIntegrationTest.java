package com.example.smartcustomerservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.smartcustomerservice.domain.dto.TicketApplySlaRequest;
import com.example.smartcustomerservice.domain.entity.Customer;
import com.example.smartcustomerservice.domain.entity.Notification;
import com.example.smartcustomerservice.domain.entity.SlaAlert;
import com.example.smartcustomerservice.domain.entity.SlaPolicy;
import com.example.smartcustomerservice.domain.entity.SysUser;
import com.example.smartcustomerservice.domain.entity.Ticket;
import com.example.smartcustomerservice.domain.entity.TicketOperationLog;
import com.example.smartcustomerservice.domain.vo.SlaAlertVO;
import com.example.smartcustomerservice.domain.vo.SlaAlertRecoveryVO;
import com.example.smartcustomerservice.mapper.auth.SysUserMapper;
import com.example.smartcustomerservice.mapper.customer.CustomerMapper;
import com.example.smartcustomerservice.mapper.notification.NotificationMapper;
import com.example.smartcustomerservice.mapper.sla.SlaAlertMapper;
import com.example.smartcustomerservice.mapper.sla.SlaPolicyMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketOperationLogMapper;
import com.example.smartcustomerservice.mq.consumer.SlaAlertFailureMessageConsumer;
import com.example.smartcustomerservice.mq.consumer.SlaAlertMessageConsumer;
import com.example.smartcustomerservice.mq.message.SlaAlertMessage;
import com.example.smartcustomerservice.mq.producer.SlaAlertMessageProducer;
import com.example.smartcustomerservice.security.LoginUserContext;
import com.example.smartcustomerservice.service.sla.SlaAlertService;
import com.example.smartcustomerservice.service.sla.SlaAlertRecoveryService;
import com.example.smartcustomerservice.service.ticket.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.rabbitmq.dynamic=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
@Import(SlaAlertIntegrationTest.RecordingProducerConfig.class)
// 使用真实数据库验证 SLA 状态流转；生产者替换为记录型测试 Bean，避免测试向实际 RabbitMQ 投递消息。
class SlaAlertIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private SlaAlertService slaAlertService;

    @Autowired
    private SlaAlertRecoveryService slaAlertRecoveryService;

    @Autowired
    private SlaAlertMessageConsumer slaAlertMessageConsumer;

    @Autowired
    private SlaAlertFailureMessageConsumer slaAlertFailureMessageConsumer;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private SlaPolicyMapper slaPolicyMapper;

    @Autowired
    private SlaAlertMapper slaAlertMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private TicketOperationLogMapper ticketOperationLogMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private RecordingSlaAlertMessageProducer slaAlertMessageProducer;

    private String testSuffix;
    private Long testUserId;
    private Long testCustomerId;

    @BeforeEach
    void setUp() {
        slaAlertMessageProducer.clear();
        testSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        testUserId = createTestUser();
        testCustomerId = createTestCustomer();
        LoginUserContext principal = new LoginUserContext(testUserId, "sla_test_" + testSuffix, List.of("ADMIN"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @AfterEach
    void cleanUp() {
        // 每个用例的数据均带有唯一测试前缀；先清理关联表，再清理工单、策略和测试账号。
        List<Ticket> tickets = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .likeRight(Ticket::getTicketNo, "SLA_TEST_" + testSuffix));
        for (Ticket ticket : tickets) {
            notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getBusinessType, "TICKET")
                    .eq(Notification::getBusinessId, ticket.getId()));
            ticketOperationLogMapper.delete(new LambdaQueryWrapper<TicketOperationLog>()
                    .eq(TicketOperationLog::getTicketId, ticket.getId()));
            slaAlertMapper.delete(new LambdaQueryWrapper<SlaAlert>()
                    .eq(SlaAlert::getTicketId, ticket.getId()));
            ticketMapper.deleteById(ticket.getId());
        }
        slaPolicyMapper.delete(new LambdaQueryWrapper<SlaPolicy>()
                .likeRight(SlaPolicy::getPolicyName, "SLA_TEST_" + testSuffix));
        if (testUserId != null) {
            sysUserMapper.deleteById(testUserId);
        }
        if (testCustomerId != null) {
            customerMapper.deleteById(testCustomerId);
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void applySlaShouldCreateFourPendingAlertsAndPublishFourDelayedMessages() {
        Ticket ticket = createTestTicket("PROCESSING", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2));
        createTestPolicy(ticket.getCategory(), ticket.getPriority());

        TicketApplySlaRequest request = new TicketApplySlaRequest();
        request.setOverwrite(true);
        request.setBaseTime(LocalDateTime.now().plusDays(1));

        ticketService.applySla(ticket.getId(), request);

        List<SlaAlert> alerts = slaAlertMapper.selectList(new LambdaQueryWrapper<SlaAlert>()
                .eq(SlaAlert::getTicketId, ticket.getId()));
        assertEquals(4, alerts.size());
        assertTrue(alerts.stream().allMatch(alert -> "PENDING".equals(alert.getStatus())));
        assertTrue(alerts.stream().anyMatch(alert -> "FIRST_RESPONSE".equals(alert.getAlertType())
                && "RISK".equals(alert.getAlertLevel())));
        assertTrue(alerts.stream().anyMatch(alert -> "FIRST_RESPONSE".equals(alert.getAlertType())
                && "OVERDUE".equals(alert.getAlertLevel())));
        assertTrue(alerts.stream().anyMatch(alert -> "RESOLVE".equals(alert.getAlertType())
                && "RISK".equals(alert.getAlertLevel())));
        assertTrue(alerts.stream().anyMatch(alert -> "RESOLVE".equals(alert.getAlertType())
                && "OVERDUE".equals(alert.getAlertLevel())));

        // applySla 的 afterCommit 回调必须为四条告警分别调用一次延迟生产者。
        assertEquals(4, slaAlertMessageProducer.getDelayedAlertIds().size());
    }

    @Test
    void consumerShouldCreateNotificationAndMarkAlertSent() {
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(10);
        Ticket ticket = createTestTicket("PROCESSING", deadline, LocalDateTime.now().plusHours(2));
        SlaAlert alert = createAlert(ticket.getId(), "FIRST_RESPONSE", "RISK", deadline);

        slaAlertMessageConsumer.consumeSlaAlert(new SlaAlertMessage(alert.getId()));

        SlaAlert storedAlert = slaAlertMapper.selectById(alert.getId());
        assertEquals("SENT", storedAlert.getStatus());
        assertNotNull(storedAlert.getNotificationId());
        assertEquals(1L, notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getBusinessId, ticket.getId())
                .eq(Notification::getType, "SLA_RISK")));
        assertEquals(1L, ticketOperationLogMapper.selectCount(new LambdaQueryWrapper<TicketOperationLog>()
                .eq(TicketOperationLog::getTicketId, ticket.getId())
                .eq(TicketOperationLog::getOperationType, "SLA_AUTO_ALERT")));
    }

    @Test
    void consumerShouldSkipAlertWhenTicketIsAlreadyResolved() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(1);
        Ticket ticket = createTestTicket("RESOLVED", LocalDateTime.now().minusHours(1), deadline);
        SlaAlert alert = createAlert(ticket.getId(), "RESOLVE", "OVERDUE", deadline);

        slaAlertMessageConsumer.consumeSlaAlert(new SlaAlertMessage(alert.getId()));

        SlaAlert storedAlert = slaAlertMapper.selectById(alert.getId());
        assertEquals("SKIPPED", storedAlert.getStatus());
        assertEquals(0L, notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getBusinessId, ticket.getId())));
    }

    @Test
    void failureConsumerShouldMarkPendingAlertFailed() {
        Ticket ticket = createTestTicket("PROCESSING", LocalDateTime.now().plusMinutes(10), LocalDateTime.now().plusHours(2));
        SlaAlert alert = createAlert(ticket.getId(), "FIRST_RESPONSE", "RISK", ticket.getFirstResponseDeadline());
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-death", List.of(Map.of("reason", "rejected", "queue", "sla.alert.trigger.queue")));

        slaAlertFailureMessageConsumer.consumeFailedSlaAlert(
                new SlaAlertMessage(alert.getId()),
                new Message(new byte[0], properties)
        );

        SlaAlert storedAlert = slaAlertMapper.selectById(alert.getId());
        assertEquals("FAILED", storedAlert.getStatus());
        assertTrue(storedAlert.getLastErrorMessage().contains("最大重试次数"));
    }

    @Test
    void retryAlertShouldResetStateAndPublishDirectRetryMessageAfterCommit() {
        Ticket ticket = createTestTicket("PROCESSING", LocalDateTime.now().plusMinutes(10), LocalDateTime.now().plusHours(2));
        SlaAlert alert = createAlert(ticket.getId(), "FIRST_RESPONSE", "RISK", ticket.getFirstResponseDeadline());
        alert.setStatus("FAILED");
        alert.setRetryCount(2);
        alert.setLastErrorMessage("模拟投递失败");
        slaAlertMapper.updateById(alert);

        SlaAlertVO result = slaAlertService.retryAlert(alert.getId());

        SlaAlert storedAlert = slaAlertMapper.selectById(alert.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("PENDING", storedAlert.getStatus());
        assertEquals(3, storedAlert.getRetryCount());
        assertEquals(null, storedAlert.getLastErrorMessage());
        assertEquals(List.of(alert.getId()), slaAlertMessageProducer.getRetryAlertIds());
    }

    @Test
    void recoveryShouldRepublishStalePendingAndProcessingAlerts() {
        LocalDateTime staleTime = LocalDateTime.now().minusMinutes(15);
        Ticket ticket = createTestTicket("PROCESSING", staleTime, staleTime);
        SlaAlert pendingAlert = createAlert(ticket.getId(), "FIRST_RESPONSE", "RISK", staleTime);
        SlaAlert processingAlert = createAlert(ticket.getId(), "RESOLVE", "OVERDUE", staleTime);
        pendingAlert.setUpdatedAt(staleTime);
        slaAlertMapper.updateById(pendingAlert);
        processingAlert.setStatus("PROCESSING");
        processingAlert.setUpdatedAt(staleTime);
        slaAlertMapper.updateById(processingAlert);

        SlaAlertRecoveryVO result = slaAlertRecoveryService.recoverStaleAlerts();

        assertEquals(1, result.getPendingRecoveredCount());
        assertEquals(1, result.getProcessingRecoveredCount());
        assertEquals(2, result.getPublishedCount());
        assertEquals("PENDING", slaAlertMapper.selectById(pendingAlert.getId()).getStatus());
        assertEquals("PENDING", slaAlertMapper.selectById(processingAlert.getId()).getStatus());
        assertEquals(2, slaAlertMessageProducer.getRetryAlertIds().size());
    }

    private Long createTestUser() {
        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername("sla_test_" + testSuffix);
        user.setPasswordHash("test-password-hash");
        user.setRealName("SLA集成测试用户");
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        sysUserMapper.insert(user);
        return user.getId();
    }

    private Long createTestCustomer() {
        LocalDateTime now = LocalDateTime.now();
        Customer customer = new Customer();
        customer.setCustomerNo("SLA_TEST_" + testSuffix);
        customer.setName("SLA集成测试客户");
        customer.setLevel("NORMAL");
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        customer.setDeleted(0);
        customerMapper.insert(customer);
        return customer.getId();
    }

    private Ticket createTestTicket(String status, LocalDateTime firstResponseDeadline, LocalDateTime resolveDeadline) {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.setTicketNo("SLA_TEST_" + testSuffix + "_" + UUID.randomUUID().toString().substring(0, 6));
        ticket.setCustomerId(testCustomerId);
        ticket.setTitle("SLA 集成测试工单");
        ticket.setContent("用于验证 SLA 自动告警链路");
        ticket.setOriginalContent("测试客户问题");
        ticket.setCategory("SLA_TEST_CATEGORY_" + testSuffix);
        ticket.setPriority("P1");
        ticket.setStatus(status);
        ticket.setReviewStatus("APPROVED");
        ticket.setSourceChannel("WEB");
        ticket.setAssigneeId(testUserId);
        ticket.setFirstResponseDeadline(firstResponseDeadline);
        ticket.setResolveDeadline(resolveDeadline);
        ticket.setCreatedBy(testUserId);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticket.setDeleted(0);
        ticketMapper.insert(ticket);
        return ticket;
    }

    private void createTestPolicy(String category, String priority) {
        LocalDateTime now = LocalDateTime.now();
        SlaPolicy policy = new SlaPolicy();
        policy.setPolicyName("SLA_TEST_" + testSuffix);
        policy.setCategory(category);
        policy.setPriority(priority);
        policy.setFirstResponseMinutes(60);
        policy.setResolveMinutes(120);
        policy.setEnabled(1);
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        policy.setDeleted(0);
        slaPolicyMapper.insert(policy);
    }

    private SlaAlert createAlert(Long ticketId, String type, String level, LocalDateTime deadlineAt) {
        LocalDateTime now = LocalDateTime.now();
        SlaAlert alert = new SlaAlert();
        alert.setTicketId(ticketId);
        alert.setAlertType(type);
        alert.setAlertLevel(level);
        alert.setStatus("PENDING");
        alert.setScheduledAt(now);
        alert.setDeadlineAt(deadlineAt);
        alert.setRetryCount(0);
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);
        slaAlertMapper.insert(alert);
        return alert;
    }

    @TestConfiguration
    static class RecordingProducerConfig {

        @Bean
        @Primary
        RecordingSlaAlertMessageProducer recordingSlaAlertMessageProducer(
                org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
            return new RecordingSlaAlertMessageProducer(rabbitTemplate);
        }
    }

    // 用内存记录替代真实 MQ 投递，避免集成测试依赖 Byte Buddy agent 或污染消息队列。
    static class RecordingSlaAlertMessageProducer extends SlaAlertMessageProducer {

        private final List<Long> delayedAlertIds = new java.util.ArrayList<>();
        private final List<Long> retryAlertIds = new java.util.ArrayList<>();

        RecordingSlaAlertMessageProducer(org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
            super(rabbitTemplate);
        }

        @Override
        public void sendDelayedAlert(SlaAlertMessage message, long delayMillis) {
            delayedAlertIds.add(message.getAlertId());
        }

        @Override
        public void sendRetryAlert(SlaAlertMessage message) {
            retryAlertIds.add(message.getAlertId());
        }

        void clear() {
            delayedAlertIds.clear();
            retryAlertIds.clear();
        }

        List<Long> getDelayedAlertIds() {
            return List.copyOf(delayedAlertIds);
        }

        List<Long> getRetryAlertIds() {
            return List.copyOf(retryAlertIds);
        }
    }
}
