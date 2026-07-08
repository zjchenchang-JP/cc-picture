package com.zjcc.ccpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.model.dto.space.SpaceAddRequest;
import com.zjcc.ccpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zjcc.ccpicturebackend.model.entity.Space;
import com.zjcc.ccpicturebackend.model.entity.SpaceUser;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.enums.SpaceLevelEnum;
import com.zjcc.ccpicturebackend.model.enums.SpaceRoleEnum;
import com.zjcc.ccpicturebackend.model.enums.SpaceTypeEnum;
import com.zjcc.ccpicturebackend.model.vo.SpaceVO;
import com.zjcc.ccpicturebackend.model.vo.UserVO;
import com.zjcc.ccpicturebackend.service.SpaceService;
import com.zjcc.ccpicturebackend.mapper.SpaceMapper;
import com.zjcc.ccpicturebackend.service.SpaceUserService;
import com.zjcc.ccpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author zjcc
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-06-17 23:03:07
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService {

    @Resource
    private UserService userService;

    // 编程式事务
    @Resource
    private TransactionTemplate transactionTemplate;

    @Lazy //解决循环依赖
    @Resource
    private SpaceUserService spaceUserService;

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 转换请求对象和实体类
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest,space);
        // 填充默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (spaceAddRequest.getSpaceType() == null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充数据
        fillSpaceBySpaceLevel(space);
        // 数据校验
        validSpace(space,true);
        // 权限校验 非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 针对用户进行加锁 TODO 分布式锁
        // 加锁 + 事务 的方式实现 同一用户只能创建一个私有空间
        // 最粗暴的方式是给空间表的 userId 加上唯一索引，但由于后续用户还可以创建团队空间，这种方式不利于扩展

        // 按用户加锁 -- 同一个用户的请求串行（同一用户同时只有一个线程在创建空间），不同用户互不影响
        // 返回字符串在常量池里的规范实例，相同字符串内容永远返回同一个对象
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        // 普通用户每类空间只能创建 1 个; 补充 spaceType 作为查询条件
                        .eq(Space::getSpaceType,spaceAddRequest.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建1个");
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                // v2.0 如果是创建团队空间 插入团队成员关联记录 创建人自动是团队空间管理员
                if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(loginUser.getId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getText());
                    // spaceUser.setCreateTime(new Date()); // 需要手动设置吗？什么注解或方式能保证自动？
                    spaceUserService.save(spaceUser);
                }
                // 返回新插入的数据id 主键回填
                return space.getId();
            });
            // 返回结果是包装类Long，可以做一些处理
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        // 创建时 前端空间名和空间级别必须传
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName),ErrorCode.PARAMS_ERROR,"空间名称不能为空");
            ThrowUtils.throwIf(null == spaceLevel,ErrorCode.PARAMS_ERROR,"空间级别不能为空");
            ThrowUtils.throwIf(null == spaceType, ErrorCode.PARAMS_ERROR, "空间类型不能为空");
        }
        // 编辑时 如果要修改空间级别或空间名
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名太长");
        }
        // 修改数据时，如果要改空间级别
        if (spaceType != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 查询关联用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize());
        if (CollectionUtils.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 封装 每个space对应的关联user -> userVo
        // 性能优化 避免N+1查询，一次性拿到所有userId
        Set<Long> userIds = spaceList.stream()
                .map(Space::getUserId)
                .filter(userId -> userId != null && userId > 0)
                .collect(Collectors.toSet());// 去重 ？？ 有必要吗，一个用户只能创建一个空间
        HashMap<Long, UserVO> userVOMap = new HashMap<>();
        // 批量查询出所有user 再转换成userVo 存入userVoMap
        if (!userIds.isEmpty()) {
            List<User> userList = userService.listByIds(userIds);
            userList.forEach(user -> userVOMap.put(user.getId(),userService.getUserVO(user)));
        }
        // 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(space -> {
                    SpaceVO spaceVO = SpaceVO.objToVo(space);
                    UserVO userVO = userVOMap.get(space.getUserId());
                    spaceVO.setUser(userVO);
                    return spaceVO;
                }).collect(Collectors.toList());
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // 没有传入 查询条件
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 拼接查询条件
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别 自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            Long maxSize = spaceLevelEnum.getMaxSize();
            Long maxCount = spaceLevelEnum.getMaxCount();
            // 如果创建空间时本身没有设置限额，才会自动填充，保证了灵活性
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 本人或管理员才有权限
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}




