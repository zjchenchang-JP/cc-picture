package com.zjcc.ccpicturebackend.mapper;

import com.zjcc.ccpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 86187
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2026-05-25 22:04:29
* @Entity com.zjcc.ccpicturebackend.model.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




