package com.zjcc.ccpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.zjcc.ccpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zjcc.ccpicturebackend.model.entity.Space;
import com.zjcc.ccpicturebackend.model.entity.SpaceUser;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.enums.SpaceRoleEnum;
import com.zjcc.ccpicturebackend.model.vo.SpaceUserVO;
import com.zjcc.ccpicturebackend.model.vo.SpaceVO;
import com.zjcc.ccpicturebackend.model.vo.UserVO;
import com.zjcc.ccpicturebackend.service.SpaceService;
import com.zjcc.ccpicturebackend.service.SpaceUserService;
import com.zjcc.ccpicturebackend.mapper.SpaceUserMapper;
import com.zjcc.ccpicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author 86187
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2026-07-05 16:41:35
*/
@Service
@AllArgsConstructor
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    private UserService userService;
    private SpaceService spaceService;

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(null == spaceUserAddRequest, ErrorCode.PARAMS_ERROR);
        // 校验
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest,spaceUser);
        validSpaceUser(spaceUser,true);
        // 操作数据库
        try {
            boolean result = this.save(spaceUser);
            ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        } catch (DuplicateKeyException e) {
            // 唯一索引触发
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复添加");
        }
        return spaceUser.getId();
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        // add 参数用来区分是创建数据时校验还是编辑时校验
        ThrowUtils.throwIf(spaceUser == null,ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            // User user = userService.getById(userId);
            // ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

            // 只判存在用 exists()/count() 更轻,不必构造整实体
            // long userCount = userService.lambdaQuery().eq(User::getId, userId).count();
            // ThrowUtils.throwIf(userCount == 0, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            boolean userExists = userService.lambdaQuery().eq(User::getId, userId).exists();
            ThrowUtils.throwIf(!userExists, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

            // Space space = spaceService.getById(spaceId);
            // ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            boolean spaceExists = spaceService.lambdaQuery().eq(Space::getId, spaceId).exists();
            ThrowUtils.throwIf(!spaceExists, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            // // 校验是否已添加该成员 没必要 并发条件下，容易被跳过，最终还是要靠 addSpaceUser 中 save 靠数据库唯一索引
            // QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
            // boolean exists = this.lambdaQuery()
            //         .eq(SpaceUser::getSpaceId, spaceId)
            //         .eq(SpaceUser::getUserId, userId)
            //         .exists();
            // ThrowUtils.throwIf(exists,ErrorCode.PARAMS_ERROR,"请勿重复添加");
        }
        // 编辑时 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        // 转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 管理查询User 和 Space
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo).collect(Collectors.toList());

        // 批量查关联 User (先滤 null 对象防 NPE, 再滤 null/非法 id)
        // 性能优化 避免N+1 查询
        Set<Long> userIdSet = spaceUserList.stream()
                .filter(Objects::nonNull)
                .map(SpaceUser::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = new HashMap<>();
        if (!userIdSet.isEmpty()) {
            userService.listByIds(userIdSet).forEach(user ->
                    userVOMap.put(user.getId(), userService.getUserVO(user)));
        }

        // 批量查关联 Space
        Set<Long> spaceIdSet = spaceUserList.stream()
                .filter(Objects::nonNull)
                .map(SpaceUser::getSpaceId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, SpaceVO> spaceVOMap = new HashMap<>();
        if (!spaceIdSet.isEmpty()) {
            spaceService.listByIds(spaceIdSet).forEach(space ->
                    spaceVOMap.put(space.getId(), SpaceVO.objToVo(space)));
        }

        // 填充关联 (get 不到就是 null)
        spaceUserVOList.forEach(spaceUserVO -> {
            spaceUserVO.setUser(userVOMap.get(spaceUserVO.getUserId()));
            spaceUserVO.setSpace(spaceVOMap.get(spaceUserVO.getSpaceId()));
        });

        return spaceUserVOList;
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }
}




