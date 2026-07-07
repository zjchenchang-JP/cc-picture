package com.zjcc.ccpicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zjcc.ccpicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.zjcc.ccpicturebackend.manager.auth.model.SpaceUserRole;
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
}
