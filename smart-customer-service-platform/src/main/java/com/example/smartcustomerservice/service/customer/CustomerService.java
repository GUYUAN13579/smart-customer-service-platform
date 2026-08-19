package com.example.smartcustomerservice.service.customer;

import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.domain.dto.CustomerCreateRequest;
import com.example.smartcustomerservice.domain.dto.CustomerQueryRequest;
import com.example.smartcustomerservice.domain.dto.CustomerUpdateRequest;
import com.example.smartcustomerservice.domain.vo.CustomerVO;

public interface CustomerService {

    CustomerVO createCustomer(CustomerCreateRequest request);

    CustomerVO updateCustomer(CustomerUpdateRequest request);

    Boolean deleteCustomer(Long id);

    CustomerVO getCustomer(Long id);

    PageResult<CustomerVO> pageCustomers(CustomerQueryRequest request);
}
