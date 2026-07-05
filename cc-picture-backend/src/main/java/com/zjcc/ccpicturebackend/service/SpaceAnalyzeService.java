package com.zjcc.ccpicturebackend.service;

import com.zjcc.ccpicturebackend.model.dto.space.analyze.*;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.vo.space.analyze.*;

import java.util.List;

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

    /**
     * 空间图片分类分析
     *
     * @param spaceCategoryAnalyzeRequest 请求参数
     * @param loginUser                   当前登录用户
     * @return List<SpaceCategoryAnalyzeResponse>
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 空间图片标签分析
     * @param spaceTagAnalyzeRequest  SpaceTagAnalyzeRequests
     * @param loginUser 登录用户
     * @return  List<SpaceTagAnalyzeResponse>
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间大小分析
     * @param spaceSizeAnalyzeRequest SpaceSizeAnalyzeRequests
     * @param loginUser               当前登录用户
     * @return List<SpaceSizeAnalyzeResponse>
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 用户上传时间分析
     * @param spaceUserAnalyzeRequest  spaceUserAnalyzeRequest
     * @param loginUser 当前登录的用户
     * @return List<SpaceUserAnalyzeResponse>
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);




}
