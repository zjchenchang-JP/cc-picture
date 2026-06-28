package com.zjcc.ccpicturebackend.service;

import com.zjcc.ccpicturebackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.zjcc.ccpicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;

public interface SpaceAnalyzeService{

    /**
     * 检验 空间分析权限
     * @param spaceAnalyzeRequest spaceAnalyzeRequest
     * @param loginUser 登录的用户
     */
    void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest 请求参数
     * @param loginUser                当前登录用户
     * @return SpaceUsageAnalyzeResponse 分析结果
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

}
