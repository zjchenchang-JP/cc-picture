package com.zjcc.ccpicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.zjcc.ccpicturebackend.model.entity.Picture;
import com.zjcc.ccpicturebackend.model.entity.Space;
import com.zjcc.ccpicturebackend.model.entity.SpaceUser;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.enums.SpaceRoleEnum;
import com.zjcc.ccpicturebackend.model.enums.SpaceTypeEnum;
import com.zjcc.ccpicturebackend.service.PictureService;
import com.zjcc.ccpicturebackend.service.SpaceService;
import com.zjcc.ccpicturebackend.service.SpaceUserService;
import com.zjcc.ccpicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.zjcc.ccpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    // 默认是application.yml 设置的  /api
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;


    /**
     * 返回一个类型账号所拥有的权限码集合
     * 不判断某操作是否符合权限，只返回 某用户对某空间的图片有哪些权限
     * 找到这个用户对这个资源的权限关系
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 判断 loginType，仅对类型为 "space" 进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            // 非 space账号相关请求，不给space账号权限
            return new ArrayList<>();
        }
        // 管理员权限，表示权限校验通过,放行
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 如果所有字段都为空，表示查询公共图库，可以通过
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        // 获取 userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();
        // 优先从上下文获取SpaceUer 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 如果有 spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        // spaceUserId 来自 /spaceUser/* 路径
        // (见 getAuthContextByRequest 的 197-199 行,moduleName=spaceUser → setSpaceUserId(id))。对应 SpaceUserController 里针对「某条成员记录」的操作:
        // POST /spaceUser/edit(编辑某成员的角色)
        // POST /spaceUser/delete(移除某成员)
        // 查某个成员信息
        // 所以"带 spaceUserId"=「要操作某条成员关系记录」
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            // 查出目标成员,拿到 目标成员 的 spaceId(知道是哪个空间)。
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            // 按 (spaceId, 当前userId) 查:确认当前登录用户在这个空间是不是成员、什么角色
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                // 当前用户不是该空间成员, 再判断是不是系统管理员(全局权限)
                // 系统管理员(userRole=admin)想管某个 team 空间的成员,但他不是这个 team 空间的成员(没有 SpaceUser 记录)
                if (userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS; // 系统管理员 → 放行
                }
                // 如果不判断isAdmin 直接返回，会导致系统管理员在私有空间没有权限
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 定位空间Space对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 图片 id 也没有，则默认通过权限校验
            Long pictureId = authContext.getPictureId();
            if (null == pictureId) {
                return ADMIN_PERMISSIONS; // 什么id参数都没有，Space账号权限无法判断，兜底放行
            }
            // 通过图片反查 spaceId
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            if (spaceId == null) {
                // 公共图库
                // 本人或管理员才能操作
                if (userService.isAdmin(loginUser) || picture.getUserId().equals(userId)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    // 公共图库中的非自己的图片, 只读权限
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        // spaceId 不为null, 获取 Space 对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间 仅本人和管理员有权限
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间, 查询 SpaceUser 并获取角色和权限
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     * 本项目不使用
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // // 本 list 仅做模拟，实际项目中要根据具体业务逻辑来查询角色
        // List<String> list = new ArrayList<String>();
        // list.add("admin");
        // list.add("super-admin");
        // return list;
        return new ArrayList<>();
    }

    /**
     * 从请求URL中获取上下文对象
     *  Hutool 的工具类 ServletUtil 从 HttpServletRequest 中获取到了参数信息
     *  但是，HttpServletRequest 的 body 值是个流，只支持读取一次，读完就没了！重复读取报错
     *  要在 config 包下自定义请求包装类和请求包装类过滤器
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        // Spring 把当前线程绑定的请求属性存进 ThreadLocal,这里取出来
        // 这个方法是被 Sa-Token 调的,不在 Controller,Spring 不会自动注入 request,只能手动从 ThreadLocal 取当前请求
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 获取请求参数
        // FORM_URLENCODED("application/x-www-form-urlencoded"),
        // MULTIPART("multipart/form-data"),
        // JSON("application/json"),
        if (ContentType.JSON.getValue().equals(contentType)) {
            // 读整个请求体字符串
            // 参数在请求体
            String body = ServletUtil.getBody(request);
            // JSON 反序列化成对象
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            // 表单/query 参数转 Map
            // 参数在request url 参数(?spaceId=5)
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        // 很多接口用 RESTful 路径传 id(DELETE /picture/123),body 里只写了 {"id":123},
        // 但这个 123 是图片 id 还是空间 id"靠 id 本身看不出来**,得看路径前缀 /picture/
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) { // 有 id 才匹配
            // 获取到请求路径的业务前缀，/api/picture/aaa?a=1
            String requestURI = request.getRequestURI();
            // 先替换掉上下文，剩下的就是前缀， picture/aaa?a=1
            String partURI = requestURI.replace(contextPath + "/", "");
            // 获取前缀的第一个斜杠前的字符串，picture
            String moduleName = StrUtil.subBefore(partURI, "/", false);
            // 根据路径里的模块名
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }

    /**
     * 判断对象的所有字段是否为空
     * 请求没带任何资源 id(列表查询等)→ 不涉及具体空间资源 → 放行(实际是查询类,写操作都带 id)
     *
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        // 啥 id 都没带(列表查询)
        // 比如：POST /picture/list/page   body: {"current":1,"pageSize":10} 直接放行
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }
}
