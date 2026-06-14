package com.zjcc.ccpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.manager.CosManager;
import com.zjcc.ccpicturebackend.manager.FileManager;
import com.zjcc.ccpicturebackend.manager.upload.FilePictureUpload;
import com.zjcc.ccpicturebackend.manager.upload.PictureUploadTemplate;
import com.zjcc.ccpicturebackend.manager.upload.UrlPictureUpload;
import com.zjcc.ccpicturebackend.model.dto.file.UploadPictureResult;
import com.zjcc.ccpicturebackend.model.dto.picture.PictureQueryRequest;
import com.zjcc.ccpicturebackend.model.dto.picture.PictureReviewRequest;
import com.zjcc.ccpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.zjcc.ccpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zjcc.ccpicturebackend.model.entity.Picture;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zjcc.ccpicturebackend.model.vo.PictureVO;
import com.zjcc.ccpicturebackend.model.vo.UserVO;
import com.zjcc.ccpicturebackend.service.PictureService;
import com.zjcc.ccpicturebackend.mapper.PictureMapper;
import com.zjcc.ccpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
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
    // private FileManager fileManager;
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片为空");
        }
        log.info("开始上传图片，用户ID = {}, pictureId = {}",
                loginUser.getId(),
                pictureUploadRequest != null ? pictureUploadRequest.getId() : null);

        ThrowUtils.throwIf(null == loginUser, ErrorCode.NO_AUTH_ERROR);
        // 判断新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片要校验图片是否存在
        if (pictureId != null) {
            // 说明是更新
            // 版本v1.0 默认是管理员才能上传
            // boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId)
            //         .exists();
            // ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // v2.0
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅上传者本人或管理员可编辑 权限校验逻辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 上传图片
        // 按照用户 id 划分 上传目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 根据 inputSource 类型区分上传方式
        // 也可以通过传一个业务参数（如 type）来区分不同的上传方式
        // 文件上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            // url上传
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        // 缩略图url地址
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        // 通过 pictureUploadRequest 对象获取到要手动设置的图片名称
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        // 如果未设置，仍依赖于解析的结果
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充审核参数
        fillReviewParams(picture, loginUser);
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新还需传入id 和编辑时间
            // URL、尺寸、大小、格式（从文件自动获取）
            // 场景：想把一张模糊的图片换成高清的 上传新图片文件替换旧的
            // 保留老记录的 id，替换图片文件相关的所有属性
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

        // v2.0 新增审核相关字段 扩展支持根据审核字段进行查询
        // 实现 “管理员筛选审核状态” 的功能
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

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
        // v1.0
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

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        // 枚举是单例，== 和 equals 效果完全一样
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断审核图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 已经是该审核状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest,updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        // 应该数据库级别的乐观锁
        // 并发审核风险
        // 两个管理员同时审核同一张图片,一个通过，一个拒绝，谁后执行谁覆盖，结果不可预期
        // 但 对于图片审核场景，并发审核的影响确实不大。因为：
        //      审核操作不涉及资金、库存等关键业务
        //      即使两个管理员同时审核，最终结果只是"以最后一个为准"
        //      不会造成数据不一致或资金损失
        // UPDATE picture SET reviewStatus = 1, reviewerId = 100, reviewTime = '2026-06-03' WHERE id = 123 AND reviewStatus = 0
        // boolean result = this.update()
        //         .eq("id", id)
        //         // 乐观锁：确保状态没被改过
        //         .eq("reviewStatus", oldPicture.getReviewStatus())
        //         .update(updatePicture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        } else {
            // 非管理员，创建或编辑都要改为待审核
            // 不能设置审核人ID, 未来具体的管理员审核时doPictureReview 再填充
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 搜索关键词，如"风景"
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 要上传的数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 批量指定图片名
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            // 名称前缀默等于搜索关键词
            namePrefix = searchText;
        }

        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        // Jsoup 抓取解析页面
        // Jsoup 是一个 Java HTML 解析库，可以像 jQuery 一样操作 DOM。
        Document document;
        try {
            // 发送 HTTP 请求，获取 HTML 文档
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 定位图片元素
        // 找到 class="dgControl" 的 div
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        // 在该 div 下查找所有 class="mimg" 的 img 标签
        Elements imgElementList = div.select("img.mimg");
        // 循环上传每张图片
        int uploadCount = 0; // 成功上传计数器
        for (Element imgElement : imgElementList) {  // 遍历每个 img 元素
            String fileUrl = imgElement.attr("src");  // 获取 src 属性（图片 URL）
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片 URL：去掉问号后的参数（防止转义问题）
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                // 示例：https://example.com/img.jpg?w=200&h=150 → https://example.com/img.jpg
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                // 设置图片名称，序号连续递增
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++; // 成功则计数+1
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue; // 失败则跳过当前图片，继续下一张
            }
            if (uploadCount >= count) { // 达到目标数量，停止循环
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // FIXME 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

}




