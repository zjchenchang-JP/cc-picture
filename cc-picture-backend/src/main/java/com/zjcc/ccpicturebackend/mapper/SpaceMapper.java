package com.zjcc.ccpicturebackend.mapper;

import com.zjcc.ccpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 86187
* @description 针对表【space(空间)】的数据库操作Mapper
* @createDate 2026-06-17 23:03:07
* @Entity com.zjcc.ccpicturebackend.model.entity.space
*/
public interface SpaceMapper extends BaseMapper<Space> {
    /**
     * 获取存储使用量排名前 N 的空间
     * @param topN 排名前 N
     * @return List<Space>
     */
    // Mapper 接口中的方法名称必须与 XML 文件中定义的 SQL 片段的 id 对应，MyBatis 才能正确解析和匹配方法。
    // Mapper 接口方法的返回类型需要与 XML 文件中 resultType（或 resultMap）的定义保持一致，以确保查询结果能够正确映射到返回对象。
    // @Select("SELECT id, spaceName, userId, totalSize " +
    //         "FROM space " +
    //         "ORDER BY totalSize DESC " +
    //         "LIMIT #{topN}")
    List<Space> getTopNSpaceUsage(int topN);

    /**
     * 删除某用户的所有空间
     *
     * @param userId 用户 ID
     * @return 删除的记录数
     */
    // @Delete("DELETE FROM space WHERE userId = #{userId}")
    int deleteByUserId(Long userId);
}




