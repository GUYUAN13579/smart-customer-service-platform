package com.example.smartcustomerservice.service.users;

import com.example.smartcustomerservice.domain.vo.CurrUserVO;
import com.example.smartcustomerservice.domain.vo.MenuVO;

import java.util.List;

// CurrUserService 属于智能客服平台基础代码。
public interface CurrUserService {

    CurrUserVO getCurrentUser();

    List<MenuVO> getCurrentMenus();
}
