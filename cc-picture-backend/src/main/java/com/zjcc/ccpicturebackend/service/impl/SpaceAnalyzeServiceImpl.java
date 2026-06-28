package com.zjcc.ccpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.zjcc.ccpicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.zjcc.ccpicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.zjcc.ccpicturebackend.model.entity.Picture;
import com.zjcc.ccpicturebackend.model.entity.Space;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.zjcc.ccpicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.zjcc.ccpicturebackend.service.PictureService;
import com.zjcc.ccpicturebackend.service.SpaceAnalyzeService;
import com.zjcc.ccpicturebackend.service.SpaceService;
import com.zjcc.ccpicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@AllArgsConstructor
@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {


    private final UserService userService;
    private final SpaceService spaceService;
    private final PictureService pictureService;

    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 权限校验
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            // 全空间分析或者公共图库权限校验：仅管理员可访问
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问公共图库");
        } else {
            // 私有空间 分析
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(null == space, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }

    }

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            // 查询全部或公共图库逻辑
            // 仅管理员可以访问
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问公共图库");

            // 统计公共图库的资源使用 要自己计算
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            if (!spaceUsageAnalyzeRequest.isQueryAll()) {
                // 公共图库 spaceId = null
                queryWrapper.isNull("spaceId");
            }
            // 性能优化: 由于只需要获取图片存储大小，从数据库中查询时要指定 只查询需要的列;
            // 使用 mapper 的 selectObjs 方法直接返回 Object 对象，而不用封装为 Picture 对象，可以提高性能并节约存储空间
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream()
                    .mapToLong(result -> result instanceof Long ? (Long) result : 0L).sum();
            long usedCount = pictureObjList.size();
            // 封装返回结果
            SpaceUsageAnalyzeResponse analyzeResponse = new SpaceUsageAnalyzeResponse();
            analyzeResponse.setUsedSize(usedSize);
            analyzeResponse.setMaxCount(usedCount);
            // 公共图库无上限、无使用比例
            analyzeResponse.setMaxSize(null);
            analyzeResponse.setSizeUsageRatio(null);
            analyzeResponse.setUsedCount(null);
            analyzeResponse.setCountUsageRatio(null);
            return analyzeResponse;
        } else {
            // 查询指定空间
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            // 获取空间信息
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "指定空间不存在");
            // 权限空间访问校验
            spaceService.checkSpaceAuth(loginUser, space);
            // 封装返回结果
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(space.getTotalSize());
            response.setMaxSize(space.getMaxSize());
            // 后端直接算好百分比，这样前端可以直接展示
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            response.setSizeUsageRatio(sizeUsageRatio);
            response.setUsedCount(space.getTotalCount());
            response.setMaxCount(space.getMaxCount());
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            response.setCountUsageRatio(countUsageRatio);
            return response;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
            SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
            User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 根据分析范围补充查询条件
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        // 使用 MyBatis-Plus 分组查询
        // 当该组所有图片的 picSize 都是 NULL 时,SUM 返回 NULL(不是 0)
        queryWrapper.select("category AS category",
                        "COUNT(*) AS count",
                        "SUM(picSize) AS totalSize")
                .groupBy("category");
        List<Map<String, Object>> categoryStatistics = pictureService.getBaseMapper().selectMaps(queryWrapper);
        // 封装返回结果
        return categoryStatistics.stream().filter(Objects::nonNull)
                .map(result -> {
                    // 封装返回结果
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    Long count = result.get("count") == null ? 0L : ((Number) result.get("count")).longValue();
                    Long totalSize = result.get("totalSize") == null ? 0L : ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                }).collect(Collectors.toList());
    }

    /**
     * 根据分析范围填充限定查询条件
     *
     * @param spaceAnalyzeRequest 请求类
     * @param queryWrapper        查询条件
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest,
                                                QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) return;
        if (spaceAnalyzeRequest.isQueryPublic()) return;
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定分析范围");
    }
}

