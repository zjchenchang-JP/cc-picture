package com.zjcc.ccpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.manager.FileManager;
import com.zjcc.ccpicturebackend.model.dto.file.UploadPictureResult;
import com.zjcc.ccpicturebackend.model.dto.picture.PictureQueryRequest;
import com.zjcc.ccpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zjcc.ccpicturebackend.model.entity.Picture;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.vo.PictureVO;
import com.zjcc.ccpicturebackend.model.vo.UserVO;
import com.zjcc.ccpicturebackend.service.PictureService;
import com.zjcc.ccpicturebackend.mapper.PictureMapper;
import com.zjcc.ccpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 86187
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-05-31 18:31:20
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        log.info("开始上传图片，用户ID = {}, pictureId = {}, 文件名 = {}",
                loginUser.getId(),
                pictureUploadRequest != null ? pictureUploadRequest.getId() : null,
                multipartFile != null ? multipartFile.getOriginalFilename() : null);

        ThrowUtils.throwIf(null == loginUser, ErrorCode.NO_AUTH_ERROR);
        // 判断新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片要校验图片是否存在
        if (pictureId != null) {
            // 说明是更新
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 上传图片
        // 按照用户 id 划分 上传目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);

        // 防御性编程：校验返回结果
        ThrowUtils.throwIf(uploadPictureResult == null, ErrorCode.SYSTEM_ERROR, "图片上传失败");

        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新还需传入id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        log.info("图片上传成功，图片ID = {}, URL = {}, 用户ID = {}", picture.getId(), picture.getUrl(), loginUser.getId());
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // searchText：前端搜索框输入的关键词，在多个字段中模糊搜索（name OR introduction）
        // 假设传入 searchText = "风景"，name = "山水"，最终 SQL :
        /*
        SELECT * FROM picture
        WHERE (name LIKE '%风景%' OR introduction LIKE '%风景%')
        AND name LIKE '%山水%'
        */
        // 所以会筛出：name 同时包含"风景"和"山水"的记录，或者 introduction 包含"风景"且 name 包含"山水"的记录
        if (StrUtil.isNotBlank(searchText)) {
            // 拼接查询条件
            // name LIKE '%风景%' OR introduction LIKE '%风景%'
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        // JSON数组查询
        if (CollUtil.isNotEmpty(tags)) {
            // 注意转义
            //  tags 在数据库中存储的是 JSON 格式的字符串，如果前端要传多个 tag（必须同时存在才查出）
            //  需要遍历 tags 数组，每个标签都使用 like 模糊查询，将这些条件组合在一起
            tags.forEach(tag -> queryWrapper.like("tags", "\"" + tag + "\""));
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象封装
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 管理查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVoPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollectionUtils.isEmpty(pictureList)) {
            // 只有分页数据 没有实际picture数据
            return pictureVoPage;
        }
        // 功能实现没问题，但是性能问题；N+1 查询问题
        // 1 次主查询 + N 次关联查询
        // getPictureVOPage 中，对每条 Picture 记录都会调用一次 userService.getById(userId)，如果一页有 10 条记录，就会产生 10 次数据库查询
        // List<PictureVO> pictureVOList = pictureList.stream().map(picture -> this.getPictureVO(picture, request)).collect(Collectors.toList());
        // pictureVoPage.setRecords(pictureVOList);

        // 优化 一次性查询关联用户
        // 1.收集所有userId
        Set<Long> userIds = pictureList.stream()
                .map(Picture::getUserId)
                .filter(userId -> userId != null && userId > 0)
                .collect(Collectors.toSet());
        HashMap<Long, UserVO> userVOMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            // 2.批量查询
            List<User> userList = userService.listByIds(userIds);
            userList.forEach(user -> userVOMap.put(user.getId(), userService.getUserVO(user)));
        }
        // 3.组装结果
        List<PictureVO> pictureVOList = pictureList.stream().map(picture -> {
            PictureVO pictureVO = PictureVO.objToVo(picture);
            UserVO userVO = userVOMap.get(picture.getUserId());
            pictureVO.setUser(userVO);
            return pictureVO;
        }).collect(Collectors.toList());
        pictureVoPage.setRecords(pictureVOList);
        return pictureVoPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }
}




