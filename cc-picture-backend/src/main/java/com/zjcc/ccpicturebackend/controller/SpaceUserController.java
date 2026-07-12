package com.zjcc.ccpicturebackend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjcc.ccpicturebackend.common.BaseResponse;
import com.zjcc.ccpicturebackend.common.DeleteRequest;
import com.zjcc.ccpicturebackend.common.ResultUtils;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.zjcc.ccpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.zjcc.ccpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.zjcc.ccpicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.zjcc.ccpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zjcc.ccpicturebackend.model.entity.Space;
import com.zjcc.ccpicturebackend.model.entity.SpaceUser;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.vo.SpaceUserVO;
import com.zjcc.ccpicturebackend.service.SpaceUserService;
import com.zjcc.ccpicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author zjchenchang
 * @createDate 2026/7/6 21:26
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
@AllArgsConstructor
public class SpaceUserController {


    private final SpaceUserService spaceUserService;
    private final UserService userService;

    /**
     * 添加成员到团队空间
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(null == spaceUserAddRequest, ErrorCode.PARAMS_ERROR);
        long result = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(result);
    }

    /**
     * 从团队空间移除成员
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(null == oldSpaceUser, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库 TODO 一并移除 Space 关联数据
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @GetMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 操作数据库
        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        SpaceUser spaceUser = spaceUserService.getOne(queryWrapper);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                               HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // DTO -> 实体类
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        spaceUserService.validSpaceUser(spaceUser, false);
        // 判断更新对象是否存在
        Long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserList);
        return ResultUtils.success(spaceUserVOList);
    }

}
