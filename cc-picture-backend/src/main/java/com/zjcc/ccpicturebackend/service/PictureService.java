package com.zjcc.ccpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjcc.ccpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zjcc.ccpicturebackend.model.dto.picture.*;
import com.zjcc.ccpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.vo.BatchEditResult;
import com.zjcc.ccpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 86187
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-05-31 18:31:20
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource 文件/url
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     * v3.0同时支持 URL 上传
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取查询的 queryWrapper
     *
     * @param pictureQueryRequest 图片请求类
     * @return 拼接完成 可用来查询的 queryWrapper
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取单个图片的 VO 封装对象
     * @param picture picture 对象
     * @param request request 请求
     * @return 对应图片的 VO
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片 VO 封装对象
     * @param picturePage  page 对象
     * @param request request 请求
     * @return 分页的 VO
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片数据校验
     * @param picture 需要校验的 picture 对象
     * 提高代码健壮性, 用于更新和修改图片时进行判断
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数，方便其他方法使用
     * @param picture   待填充的 picture参数
     * @param loginUser 登录的用户
     * 图片上传、用户编辑、管理员更新这 3 个操作都需要设置审核状态，
     * 所以抽取一个通用的 “补充审核参数” 的方法，根据用户的角色给图片对象填充审核字段的值
     */
    void fillReviewParams(Picture picture, User loginUser);


    /**
     * 从Bing.com 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 图片清理
     * @param oldPicture
     * 可以将clearPictureFile 方法运用到图片删除接口 等场景
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 校验是否有图片的 编辑or删除权限
     * @param loginUser
     * @param picture
     * 有删除权限就有编辑权限）
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 按颜色搜索图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量更新
     * 处理的数据量不大, 用本方法
     * @param pictureEditByBatchRequest pictureEditByBatchRequest
     * @param loginUser                 登录的用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * (大量数据) 批量编辑图片分类、标签、重命名
     * 性能优化: 线程池 + 分批 + 并发进行优化
     * @param pictureEditByBatchRequest pictureEditByBatchRequest
     * @param loginUser 登录的用户
     */
    BatchEditResult batchEditPictureMetadata(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * AI 扩展图片服务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                                               User loginUser);
}
