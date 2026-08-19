package com.example.smartcustomerservice.service.users;

import com.example.smartcustomerservice.domain.vo.CurrUserVO;
import com.example.smartcustomerservice.domain.vo.MenuVO;

import java.util.List;

public interface CurrUserService {

    CurrUserVO getCurrentUser();

    List<MenuVO> getCurrentMenus();
}
