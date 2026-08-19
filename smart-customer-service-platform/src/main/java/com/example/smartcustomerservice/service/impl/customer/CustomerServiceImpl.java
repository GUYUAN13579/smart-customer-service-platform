package com.example.smartcustomerservice.service.impl.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.CustomerCreateRequest;
import com.example.smartcustomerservice.domain.dto.CustomerQueryRequest;
import com.example.smartcustomerservice.domain.dto.CustomerUpdateRequest;
import com.example.smartcustomerservice.domain.entity.AuditLog;
import com.example.smartcustomerservice.domain.entity.Customer;
import com.example.smartcustomerservice.domain.vo.CustomerVO;
import com.example.smartcustomerservice.mapper.auth.AuditLogMapper;
import com.example.smartcustomerservice.mapper.customer.CustomerMapper;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.customer.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final String DEFAULT_LEVEL = "NORMAL";
    private static final DateTimeFormatter CUSTOMER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final CustomerMapper customerMapper;
    private final AuditLogMapper auditLogMapper;

    public CustomerServiceImpl(CustomerMapper customerMapper, AuditLogMapper auditLogMapper) {
        this.customerMapper = customerMapper;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerVO createCustomer(CustomerCreateRequest request) {
        String customerNo = StringUtils.hasText(request.getCustomerNo())
                ? request.getCustomerNo()
                : generateCustomerNo();

        checkUnique(customerNo, request.getPhone(), request.getEmail(), null);

        LocalDateTime now = LocalDateTime.now();
        Customer customer = new Customer();
        customer.setCustomerNo(customerNo);
        customer.setName(request.getName());
        customer.setPhone(blankToNull(request.getPhone()));
        customer.setEmail(blankToNull(request.getEmail()));
        customer.setLevel(defaultLevel(request.getLevel()));
        customer.setTags(blankToNull(request.getTags()));
        customer.setRemark(blankToNull(request.getRemark()));
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        customer.setDeleted(CommonConstants.NOT_DELETED);
        customerMapper.insert(customer);

        saveAuditLog("CREATE", customer.getId(), customer.getName());
        return toVO(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerVO updateCustomer(CustomerUpdateRequest request) {
        Customer existing = customerMapper.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在");
        }

        String customerNo = StringUtils.hasText(request.getCustomerNo())
                ? request.getCustomerNo()
                : existing.getCustomerNo();
        checkUnique(customerNo, request.getPhone(), request.getEmail(), request.getId());

        existing.setCustomerNo(customerNo);
        existing.setName(request.getName());
        existing.setPhone(blankToNull(request.getPhone()));
        existing.setEmail(blankToNull(request.getEmail()));
        existing.setLevel(defaultLevel(request.getLevel()));
        existing.setTags(blankToNull(request.getTags()));
        existing.setRemark(blankToNull(request.getRemark()));
        existing.setUpdatedAt(LocalDateTime.now());
        customerMapper.updateById(existing);

        saveAuditLog("UPDATE", existing.getId(), existing.getName());
        return toVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCustomer(Long id) {
        Customer existing = customerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在");
        }

        boolean deleted = customerMapper.deleteById(id) > 0;
        if (deleted) {
            saveAuditLog("DELETE", id, existing.getName());
        }
        return deleted;
    }

    @Override
    public CustomerVO getCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在");
        }
        return toVO(customer);
    }

    @Override
    public PageResult<CustomerVO> pageCustomers(CustomerQueryRequest request) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .and(StringUtils.hasText(request.getKeyword()), query -> query
                        .like(Customer::getCustomerNo, request.getKeyword())
                        .or()
                        .like(Customer::getName, request.getKeyword())
                        .or()
                        .like(Customer::getPhone, request.getKeyword())
                        .or()
                        .like(Customer::getEmail, request.getKeyword()))
                .eq(StringUtils.hasText(request.getLevel()), Customer::getLevel, request.getLevel())
                .orderByDesc(Customer::getUpdatedAt)
                .orderByDesc(Customer::getId);

        Page<Customer> page = customerMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
        List<CustomerVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private void checkUnique(String customerNo, String phone, String email, Long excludeId) {
        if (existsByField(Customer::getCustomerNo, customerNo, excludeId)) {
            throw new BusinessException(ResultCode.CONFLICT, "客户编号已存在");
        }
        if (StringUtils.hasText(phone) && existsByField(Customer::getPhone, phone, excludeId)) {
            throw new BusinessException(ResultCode.CONFLICT, "手机号已存在");
        }
        if (StringUtils.hasText(email) && existsByField(Customer::getEmail, email, excludeId)) {
            throw new BusinessException(ResultCode.CONFLICT, "邮箱已存在");
        }
    }

    private boolean existsByField(com.baomidou.mybatisplus.core.toolkit.support.SFunction<Customer, ?> field,
                                  String value,
                                  Long excludeId) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(field, value)
                .ne(excludeId != null, Customer::getId, excludeId)
                .last("LIMIT 1");
        return customerMapper.selectCount(wrapper) > 0;
    }

    private CustomerVO toVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        vo.setId(customer.getId());
        vo.setCustomerNo(customer.getCustomerNo());
        vo.setName(customer.getName());
        vo.setPhone(customer.getPhone());
        vo.setEmail(customer.getEmail());
        vo.setLevel(customer.getLevel());
        vo.setTags(customer.getTags());
        vo.setRemark(customer.getRemark());
        vo.setCreatedAt(customer.getCreatedAt());
        vo.setUpdatedAt(customer.getUpdatedAt());
        return vo;
    }

    private void saveAuditLog(String action, Long customerId, String customerName) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOperatorId(SecurityUtils.getCurrentUserId());
        auditLog.setAction(action);
        auditLog.setResourceType("CUSTOMER");
        auditLog.setResourceId(String.valueOf(customerId));
        auditLog.setDetail("{\"customerId\":" + customerId + ",\"customerName\":\"" + escapeJson(customerName) + "\"}");
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
    }

    private String generateCustomerNo() {
        return "C" + LocalDateTime.now().format(CUSTOMER_NO_FORMATTER);
    }

    private String defaultLevel(String level) {
        return StringUtils.hasText(level) ? level : DEFAULT_LEVEL;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
