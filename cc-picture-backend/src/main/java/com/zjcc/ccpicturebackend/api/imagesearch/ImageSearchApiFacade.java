package com.zjcc.ccpicturebackend.api.imagesearch;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import com.zjcc.ccpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.zjcc.ccpicturebackend.api.imagesearch.sub.ImageSearchSeleniumApi;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

/**
 * 以图搜图 - 门面模式
 * <p>
 * 封装"下载图片 -> Selenium 搜图 -> 清理临时文件"的完整流程, 供 Controller 直接调用。
 * 百度 graph.baidu.com 接口对非浏览器环境一律拒绝, 必须用 Selenium 起真实浏览器; 而 Selenium
 * 的文件上传只接受本地路径, 所以这里先把图片 URL 下载到临时文件, 再交给 Selenium。
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 以图搜图
     *
     * @param imageUrl 图片 URL
     * @return 相似图列表
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        File tempFile = null;
        try {
            // 1. Selenium 只接受本地图片路径, 先把图片 URL 下载到临时文件
            tempFile = FileUtil.createTempFile("image-search-", ".jpg", null, true);
            byte[] bytes = HttpRequest.get(imageUrl).timeout(15000).execute().bodyBytes();
            FileUtil.writeBytes(bytes, tempFile);
            // 2. 调 Selenium 以图搜图
            return ImageSearchSeleniumApi.searchSimilarImages(tempFile.getAbsolutePath());
        } catch (BusinessException e) {
            // 业务异常(如 Selenium 搜索失败)直接透传
            throw e;
        } catch (Exception e) {
            log.error("以图搜图失败, imageUrl={}", imageUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "以图搜图失败");
        } finally {
            // 3. 清理临时文件
            if (tempFile != null) {
                FileUtil.del(tempFile);
            }
        }
    }
}
