package com.example.smartcustomerservice.controller.users;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.domain.vo.CurrUserVO;
import com.example.smartcustomerservice.domain.vo.MenuVO;
import com.example.smartcustomerservice.service.users.CurrUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "当前用户信息")
@RestController
@RequestMapping(CommonConstants.API_PREFIX)
public class CurrUserController {

    private final CurrUserService currUserService;

    public CurrUserController(CurrUserService currUserService) {
        this.currUserService = currUserService;
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/users/me")
    public ApiResult<CurrUserVO> me() {
        return ApiResult.success(currUserService.getCurrentUser());
    }

    @Operation(summary = "当前用户菜单")
    @GetMapping("/menus/me")
    public ApiResult<List<MenuVO>> menus() {
        return ApiResult.success(currUserService.getCurrentMenus());
    }
}
