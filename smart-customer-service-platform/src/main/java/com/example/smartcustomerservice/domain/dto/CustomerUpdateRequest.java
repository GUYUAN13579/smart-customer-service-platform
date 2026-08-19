package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerUpdateRequest {

    private Long id;

    @Size(max = 32, message = "客户编号最多 32 位")
    private String customerNo;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 128, message = "客户名称最多 128 位")
    private String name;

    @Size(max = 32, message = "手机号最多 32 位")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱最多 128 位")
    private String email;

    @Pattern(regexp = "NORMAL|VIP|ENTERPRISE", message = "客户等级只能是 NORMAL、VIP 或 ENTERPRISE")
    private String level = "NORMAL";

    @Size(max = 512, message = "标签最多 512 位")
    private String tags;

    @Size(max = 512, message = "备注最多 512 位")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public void setCustomerNo(String customerNo) {
        this.customerNo = customerNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
