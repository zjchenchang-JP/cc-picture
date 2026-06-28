package com.zjcc.ccpicturebackend.mapper;

import com.zjcc.ccpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @author 86187
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2026-05-31 18:31:20
* @Entity com.zjcc.ccpicturebackend.model.entity.Picture
*/
public interface PictureMapper extends BaseMapper<Picture> {

    /**
     * 查询图片分类统计信息
     *
     * @param spaceId 空间 ID（可选），用于筛选特定空间的分类统计数据；如果为空，查询所有空间的统计数据
     * @return 分类统计列表，每个 Map 包含以下字段：
     * - category: 分类名
     * - count: 分类下的图片数量
     * - totalSize: 分类下图片总大小
     */
    // @MapKey("category")
    List<Map<String, Object>> getCategoryStatistics(@Param("spaceId") Long spaceId);

}




