package com.zjcc.ccpicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zjcc.ccpicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.zjcc.ccpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.zjcc.ccpicturebackend.manager.auth.model.SpaceUserRole;
import com.zjcc.ccpicturebackend.model.entity.Space;
import com.zjcc.ccpicturebackend.model.entity.SpaceUser;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.enums.SpaceRoleEnum;
import com.zjcc.ccpicturebackend.model.enums.SpaceTypeEnum;
import com.zjcc.ccpicturebackend.service.SpaceUserService;
import com.zjcc.ccpicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 引入团队空间后，需要给空间操作、图片操作、空间成员操作添加权限控制逻辑
 * 根据 RBAC 权限模型，需要定义角色和权限
 * 用 spaceUserAuthConfig.json 配置文件来定义角色、权限、角色和权限之间的关系，
 * 相比从数据库表中获取，实现更方便，查询也更高效
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        // 加载配置文件到对象
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }
        // 找到匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();
    }

    /**
     * 获取 权限列表
     * 前端也需要根据用户的权限来进行一些页面内容的展示和隐藏
     * 后端需要将用户具有的权限返回给前端，这样就不用让前端编写复杂的角色和权限校验逻辑
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        // 未登录无权限
        if (loginUser == null) return new ArrayList<>();
        // 管理员权限
        List<String> ADMIN_PERMISSION = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) return ADMIN_PERMISSION;
            return new ArrayList<>();
        }
        //  根据空间类型获取对应的权限
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) return new ArrayList<>();
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (userService.isAdmin(loginUser) || loginUser.getId().equals(space.getUserId())) {
                    return ADMIN_PERMISSION;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

}
