package com.zjcc.ccpicturebackend.controller;

import com.zjcc.ccpicturebackend.annotation.AuthCheck;
import com.zjcc.ccpicturebackend.common.BaseResponse;
import com.zjcc.ccpicturebackend.common.ResultUtils;
import com.zjcc.ccpicturebackend.constant.UserConstant;
import com.zjcc.ccpicturebackend.model.enums.UserRoleEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
