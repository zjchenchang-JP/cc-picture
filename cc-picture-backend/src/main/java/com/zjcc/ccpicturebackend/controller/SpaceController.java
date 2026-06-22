package com.zjcc.ccpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.zjcc.ccpicturebackend.annotation.AuthCheck;
import com.zjcc.ccpicturebackend.common.BaseResponse;
import com.zjcc.ccpicturebackend.common.DeleteRequest;
import com.zjcc.ccpicturebackend.common.ResultUtils;
import com.zjcc.ccpicturebackend.constant.UserConstant;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.model.dto.space.SpaceAddRequest;
import com.zjcc.ccpicturebackend.model.dto.space.SpaceUpdateRequest;
import com.zjcc.ccpicturebackend.model.entity.Space;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.service.SpaceService;
import com.zjcc.ccpicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/space")
@AllArgsConstructor // 所有字段都作为参数 自动注入.无需@Resource
public class SpaceController {

    private final SpaceService spaceService;

    private final UserService userService;

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);
    }

    /**
     * 删除空间
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest
            , HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 判断要删除的空间是否存在
        Long spaceId = deleteRequest.getId();
        Space oldSpace = spaceService.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null,ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        spaceService.checkSpaceAuth(loginUser,oldSpace);
        boolean result = spaceService.removeById(spaceId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 更新接口 - 仅管理员
     * @param spaceUpdateRequest 前端请求
     * @return Boolean true 是成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(
            @RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断更新空间对象是否存在
        Long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(null == oldSpace,ErrorCode.NOT_FOUND_ERROR);
        // 转换请求对象和实体类
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest,space);
        // 参数校验和数据填充
        spaceService.validSpace(space,false);
        spaceService.fillSpaceBySpaceLevel(space);
        // 执行更新
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}

