package com.zjcc.ccpicturebackend.service;

import com.zjcc.ccpicturebackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.zjcc.ccpicturebackend.model.entity.User;

public interface SpaceAnalyzeService{

    /**
     * 检验空间分析 权限
     * @param spaceAnalyzeRequest spaceAnalyzeRequest
     * @param loginUser 登录的用户
     */
    void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);
}
