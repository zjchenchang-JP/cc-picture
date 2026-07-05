package com.zjcc.ccpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.model.dto.space.analyze.*;
import com.zjcc.ccpicturebackend.model.entity.Picture;
import com.zjcc.ccpicturebackend.model.entity.Space;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.vo.space.analyze.*;
import com.zjcc.ccpicturebackend.service.PictureService;
import com.zjcc.ccpicturebackend.service.SpaceAnalyzeService;
import com.zjcc.ccpicturebackend.service.SpaceService;
import com.zjcc.ccpicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
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

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限检测
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest,loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest,queryWrapper);
        // 查询所有符合条件的标签
        queryWrapper.select("tags");
        List<Object> selectObjs = pictureService.getBaseMapper().selectObjs(queryWrapper);
        List<String> tagsJsonList = selectObjs.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        // 合并所有标签并统计使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .filter(str -> !str.isBlank())// 过滤空的 tags字段
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .filter(StrUtil::isNotBlank)    // 过滤tags中的空标签 ""
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 按使用次数降序排序, 转换为响应对象
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(e -> new SpaceTagAnalyzeResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest,queryWrapper);
        // 查询符合条件的图片 大小
        queryWrapper.select("picSize");
        List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream().map(sizeObject -> ((Number) sizeObject).longValue())
                .collect(Collectors.toList());
        // 预初始化所有区间为 0: 保证顺序固定(由 put 顺序决定) + 空区间也出现
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", 0L);
        sizeRanges.put("100KB-500KB", 0L);
        sizeRanges.put("500KB-1MB", 0L);
        sizeRanges.put(">1MB", 0L);
        // 只遍历一次 picSizes, 每张图 O(1) 落入对应区间累加(else if 级联, 阈值不重复)
        picSizes.forEach(picSize -> {
            if (picSize < 100 * 1024) {
                sizeRanges.merge("<100KB", 1L, Long::sum);
            } else if (picSize < 500 * 1024) {
                sizeRanges.merge("100KB-500KB", 1L, Long::sum);
            } else if (picSize < 1024 * 1024) {
                sizeRanges.merge("500KB-1MB", 1L, Long::sum);
            } else {
                sizeRanges.merge(">1MB", 1L, Long::sum);
            }
        });
        // 封装响应对象
        return sizeRanges.entrySet()
                .stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {

        // 参数校验
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest,loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(Objects.nonNull(userId), "userId", userId);
        // 分析维度 每日 每周 每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        //  MySQL 日期时间函数对图片的创建时间进行格式化，使同一天（周 / 月）的值相同，就能够统一按照一个字段（period）进行分组和排序了
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        // 分组查询 和 排序
        queryWrapper.groupBy("period").orderByAsc("period");
        // 查询结果并转换
        List<Map<String, Object>> selectedMaps = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return selectedMaps.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                }).collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 仅管理员可查看空间排行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser),ErrorCode.NO_AUTH_ERROR,"无权限查看空间排行");
        // 构造查询条件
        //  SELECT id, spaceName, userId, totalSize
        //  FROM space
        //  ORDER BY totalSize DESC
        //  LIMIT #{topN}
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());// 取前 N 名
        return spaceService.list(queryWrapper);
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
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定分析范围");
    }
}

