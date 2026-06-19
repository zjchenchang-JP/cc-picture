package com.zjcc.ccpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.enums.SpaceLevelEnum;
import com.zjcc.ccpicturebackend.model.vo.SpaceVO;
import com.zjcc.ccpicturebackend.service.SpaceService;
import com.zjcc.ccpicturebackend.mapper.SpaceMapper;
import com.zjcc.ccpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

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
        // 填充数据
        fillSpaceBySpaceLevel(space);
        // 数据校验
        validSpace(space,true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验 非管理员只能创建普通级别的空间
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
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能有1个私有空间");
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
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
        // 创建时 前端空间名和空间级别必须传
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName),ErrorCode.PARAMS_ERROR,"空间名称不能为空");
            ThrowUtils.throwIf(null == spaceLevel,ErrorCode.PARAMS_ERROR,"空间级别不能为空");
        }
        // 编辑时 如果要修改空间级别或空间名
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名太长");
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        return null;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        return null;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return null;
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
}




