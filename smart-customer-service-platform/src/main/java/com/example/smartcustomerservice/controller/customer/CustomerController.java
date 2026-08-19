package com.example.smartcustomerservice.controller.customer;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.CustomerCreateRequest;
import com.example.smartcustomerservice.domain.dto.CustomerQueryRequest;
import com.example.smartcustomerservice.domain.dto.CustomerUpdateRequest;
import com.example.smartcustomerservice.domain.vo.CustomerVO;
import com.example.smartcustomerservice.service.customer.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "客户管理")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "创建客户")
    @PostMapping
    @PreAuthorize("hasAuthority('customer:create')")
    public ApiResult<CustomerVO> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        return ApiResult.success(customerService.createCustomer(request));
    }

    @Operation(summary = "修改客户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:update')")
    public ApiResult<CustomerVO> updateCustomer(@NotNull(message = "客户ID不能为空") @PathVariable Long id,
                                                @Valid @RequestBody CustomerUpdateRequest request) {
        request.setId(id);
        return ApiResult.success(customerService.updateCustomer(request));
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:delete')")
    public ApiResult<Boolean> deleteCustomer(@NotNull(message = "客户ID不能为空") @PathVariable Long id) {
        return ApiResult.success(customerService.deleteCustomer(id));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:detail')")
    public ApiResult<CustomerVO> getCustomer(@NotNull(message = "客户ID不能为空") @PathVariable Long id) {
        return ApiResult.success(customerService.getCustomer(id));
    }

    @Operation(summary = "客户分页查询")
    @GetMapping
    @PreAuthorize("hasAuthority('customer:list')")
    public ApiResult<PageResult<CustomerVO>> pageCustomers(@Valid CustomerQueryRequest request) {
        return ApiResult.success(customerService.pageCustomers(request));
    }
}
