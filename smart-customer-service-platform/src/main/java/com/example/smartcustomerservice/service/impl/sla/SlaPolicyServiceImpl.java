package com.example.smartcustomerservice.service.impl.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.SlaPolicyCreateRequest;
import com.example.smartcustomerservice.domain.dto.SlaPolicyQueryRequest;
import com.example.smartcustomerservice.domain.dto.SlaPolicyUpdateRequest;
import com.example.smartcustomerservice.domain.entity.SlaPolicy;
import com.example.smartcustomerservice.domain.vo.SlaPolicyVO;
import com.example.smartcustomerservice.mapper.sla.SlaPolicyMapper;
import com.example.smartcustomerservice.service.sla.SlaPolicyService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
// SlaPolicyServiceImpl 属于智能客服平台基础代码。
public class SlaPolicyServiceImpl implements SlaPolicyService {

    private final SlaPolicyMapper slaPolicyMapper;

    public SlaPolicyServiceImpl(SlaPolicyMapper slaPolicyMapper) {
        this.slaPolicyMapper = slaPolicyMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SlaPolicyVO createPolicy(SlaPolicyCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求参数不能为空");
        }
        String category = blankToNull(request.getCategory());
        String priority = request.getPriority();

        // 同一分类和优先级只能有一条启用策略，保证工单应用 SLA 时能得到唯一规则。
        Long count = slaPolicyMapper.selectCount(buildDuplicateWrapper(category, priority, null));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "同分类、同优先级已有启用策略");
        }

        LocalDateTime now = LocalDateTime.now();
        SlaPolicy slaPolicy = new SlaPolicy();
        slaPolicy.setPolicyName(request.getPolicyName());
        slaPolicy.setCategory(category);
        slaPolicy.setPriority(priority);
        slaPolicy.setFirstResponseMinutes(request.getFirstResponseMinutes());
        slaPolicy.setResolveMinutes(request.getResolveMinutes());
        slaPolicy.setEnabled(defaultOne(request.getEnabled()));
        slaPolicy.setCreatedAt(now);
        slaPolicy.setUpdatedAt(now);
        slaPolicy.setDeleted(0);

        int flag = slaPolicyMapper.insert(slaPolicy);
        if (flag == 0) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "数据库操作失败");
        }
        return toVO(slaPolicy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SlaPolicyVO updatePolicy(SlaPolicyUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SLA策略ID不能为空");
        }
        SlaPolicy existing = getExistingPolicy(request.getId());
        String category = blankToNull(request.getCategory());
        String priority = request.getPriority();

        Long count = slaPolicyMapper.selectCount(buildDuplicateWrapper(category, priority, request.getId()));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "同分类、同优先级已有启用策略");
        }

        existing.setPolicyName(request.getPolicyName());
        existing.setCategory(category);
        existing.setPriority(priority);
        existing.setFirstResponseMinutes(request.getFirstResponseMinutes());
        existing.setResolveMinutes(request.getResolveMinutes());
        existing.setEnabled(defaultOne(request.getEnabled()));
        existing.setUpdatedAt(LocalDateTime.now());

        int flag = slaPolicyMapper.updateById(existing);
        if (flag == 0) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "数据库操作失败");
        }
        return toVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deletePolicy(Long id) {
        getExistingPolicy(id);
        int flag = slaPolicyMapper.update(null, new LambdaUpdateWrapper<SlaPolicy>()
                .eq(SlaPolicy::getId, id)
                .eq(SlaPolicy::getDeleted, 0)
                .set(SlaPolicy::getDeleted, 1)
                .set(SlaPolicy::getUpdatedAt, LocalDateTime.now()));
        return flag > 0;
    }

    @Override
    public SlaPolicyVO getPolicy(Long id) {
        return toVO(getExistingPolicy(id));
    }

    @Override
    public PageResult<SlaPolicyVO> pagePolicies(SlaPolicyQueryRequest request) {
        if (request == null) {
            request = new SlaPolicyQueryRequest();
        }
        String keyword = request.getKeyword();
        String category = request.getCategory();
        String priority = request.getPriority();
        Integer enabled = request.getEnabled();
        // 关键词条件放在括号内，生成 (name LIKE ? OR category LIKE ? OR priority LIKE ?) 的查询语义。
        LambdaQueryWrapper<SlaPolicy> wrapper = new LambdaQueryWrapper<SlaPolicy>()
                .eq(SlaPolicy::getDeleted, 0)
                .and(StringUtils.hasText(keyword), item -> item
                        .like(SlaPolicy::getPolicyName, keyword)
                        .or()
                        .like(SlaPolicy::getCategory, keyword)
                        .or()
                        .like(SlaPolicy::getPriority, keyword))
                .eq(StringUtils.hasText(category), SlaPolicy::getCategory, category)
                .eq(StringUtils.hasText(priority), SlaPolicy::getPriority, priority)
                .eq(enabled != null, SlaPolicy::getEnabled, enabled)
                .orderByDesc(SlaPolicy::getUpdatedAt)
                .orderByDesc(SlaPolicy::getId);

        Page<SlaPolicy> page = slaPolicyMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
        List<SlaPolicyVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private SlaPolicy getExistingPolicy(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SLA策略ID不能为空");
        }
        SlaPolicy policy = slaPolicyMapper.selectOne(new LambdaQueryWrapper<SlaPolicy>()
                .eq(SlaPolicy::getId, id)
                .eq(SlaPolicy::getDeleted, 0));
        if (policy == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SLA策略不存在");
        }
        return policy;
    }

    private LambdaQueryWrapper<SlaPolicy> buildDuplicateWrapper(String category, String priority, Long excludeId) {
        // category 为 null 代表默认策略，不能用 eq(null) 替代 isNull，否则无法可靠判断重复策略。
        LambdaQueryWrapper<SlaPolicy> wrapper = new LambdaQueryWrapper<SlaPolicy>()
                .eq(SlaPolicy::getPriority, priority)
                .eq(SlaPolicy::getEnabled, 1)
                .eq(SlaPolicy::getDeleted, 0)
                .ne(excludeId != null, SlaPolicy::getId, excludeId);
        if (StringUtils.hasText(category)) {
            wrapper.eq(SlaPolicy::getCategory, category);
        } else {
            wrapper.isNull(SlaPolicy::getCategory);
        }
        return wrapper;
    }

    private SlaPolicyVO toVO(SlaPolicy slaPolicy) {
        SlaPolicyVO slaPolicyVO = new SlaPolicyVO();
        BeanUtils.copyProperties(slaPolicy, slaPolicyVO);
        return slaPolicyVO;
    }

    private Integer defaultOne(Integer value) {
        return value == null ? 1 : value;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
