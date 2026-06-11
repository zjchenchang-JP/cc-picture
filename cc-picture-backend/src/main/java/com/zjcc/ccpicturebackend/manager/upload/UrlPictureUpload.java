package com.zjcc.ccpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {

    /**
     * 缓存当前 URL 对应的 Content-Type
     * key: URL, value: Content-Type (如 "image/jpeg")
     */
    private static final java.util.Map<String, String> URL_CONTENT_TYPE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        try {
            // 1.校验url格式
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL格式不正确");
        }
        // 2. 校验URL协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 3. 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回 无需后续验证
            // 有些 URL 地址可能不支持通过 HEAD 请求访问，为了提高导入成功率，即使 HEAD 请求访问失败，也不会报错
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 4 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
                // 缓存 Content-Type，后续生成文件名时使用
                URL_CONTENT_TYPE_CACHE.put(fileUrl, contentType);
            }
            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    // 限制文件大小为 2MB
                    final long ONE_M = 1024 * 1024L;
                    ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }  
  
    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 去掉 URL 参数
        int questionMarkIndex = fileUrl.indexOf("?");
        if (questionMarkIndex > -1) {
            fileUrl = fileUrl.substring(0, questionMarkIndex);
        }

        // 从缓存中获取 Content-Type
        String contentType = URL_CONTENT_TYPE_CACHE.get(fileUrl);
        if (StrUtil.isNotBlank(contentType)) {
            // 根据 Content-Type 生成正确的文件名
            String extension = contentTypeToExtension(contentType);
            if (StrUtil.isNotBlank(extension)) {
                // 对于 Bing 等 URL，提取 ID 部分
                String fileName = extractFileNameFromUrl(fileUrl);
                return fileName + "." + extension;
            }
        }

        // 如果没有 Content-Type，返回原始 URL（兼容旧逻辑）
        return fileUrl;
    }

    /**
     * 从 URL 中提取文件名（去掉路径）
     * 例如：https://example.com/path/to/OIP.xyz → OIP.xyz
     */
    private String extractFileNameFromUrl(String url) {
        int lastSlashIndex = url.lastIndexOf("/");
        if (lastSlashIndex > -1 && lastSlashIndex < url.length() - 1) {
            return url.substring(lastSlashIndex + 1);
        }
        return url;
    }

    /**
     * 将 Content-Type 转换为文件扩展名
     */
    private String contentTypeToExtension(String contentType) {
        if (contentType == null) {
            return null;
        }
        switch (contentType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/webp":
                return "webp";
            default:
                return null;
        }
    }  
  
    @Override  
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;  
        // 下载文件到临时目录  
        HttpUtil.downloadFile(fileUrl, file);
    }  
}
