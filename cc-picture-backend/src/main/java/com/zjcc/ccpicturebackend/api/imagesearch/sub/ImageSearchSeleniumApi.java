package com.zjcc.ccpicturebackend.api.imagesearch.sub;

import com.zjcc.ccpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 以图搜图 - Selenium 原型
 * <p>
 * 百度识图 graph.baidu.com/upload 接口对非浏览器环境的调用一律拒绝(Params illegal / Reject),
 * 纯 HTTP 方案不可行, 只能用真实浏览器绕过风控。
 * <p>
 * 本类用 Edge 驱动百度识图页面: 打开首页 -> 上传本地图片 -> 抓取相似图列表。
 * Selenium 4 自带 Selenium Manager, 会自动下载匹配本机 Edge 的 msedgedriver, 无需手动管理驱动。
 * <p>
 * 注意: 结果列表的精确 DOM 选择器尚未校准, 当前用宽松策略抓 img,
 *      跑通后根据真实页面 HTML 再换成精确 selector。
 */
@Slf4j
public class ImageSearchSeleniumApi {

    /**
     * 百度识图首页
     */
    private static final String HOME_URL = "https://graph.baidu.com/pcpage/index?tpl_from=pc";

    /**
     * 上传成功后, 新版百度结果页 URL 跳转到 graph.baidu.com/s?... 并带 session_id 参数,
     * 用它判断是否进入结果页 (旧版是 card=, 现已改版)
     */
    private static final String RESULT_FLAG = "session_id=";

    /**
     * 用本地图片做以图搜图, 返回相似图列表
     *
     * @param imagePath 本地图片的【绝对路径】
     */
    public static List<ImageSearchResult> searchSimilarImages(String imagePath) {
        setupDriverPath();
        WebDriver driver = new EdgeDriver(buildEdgeOptions());
        try {
            injectAntiDetection(driver);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // 1. 打开百度识图首页
            driver.get(HOME_URL);
            log.info("[step1] 已打开识图首页 title={} url={}", driver.getTitle(), driver.getCurrentUrl());

            // 2. 定位并上传文件 input
            List<WebElement> fileInputs = driver.findElements(By.cssSelector("input[type='file']"));
            log.info("[step2] 找到 input[type=file] 数量={}", fileInputs.size());
            for (int i = 0; i < fileInputs.size(); i++) {
                WebElement fi = fileInputs.get(i);
                log.info("  input#{} id={} name={} accept={}",
                        i, fi.getAttribute("id"), fi.getAttribute("name"), fi.getAttribute("accept"));
            }
            WebElement fileInput = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")));
            fileInput.sendKeys(imagePath);
            log.info("[step2] 已上传图片 path={}", imagePath);

            // 3. 等待跳转到结果页 (URL 出现 session_id=)
            wait.until(d -> d.getCurrentUrl().contains(RESULT_FLAG));
            log.info("[step3] 已进入结果页 url={}", driver.getCurrentUrl());
            // 结果页相似图由 JS 异步加载, 等待几秒让列表渲染完成
            sleepMillis(3000);

            // 4. 抓取相似图列表 + 落盘 HTML 供下一轮校准
            List<ImageSearchResult> results = extractImages(driver);
            log.info("[step4] 抓取到候选相似图数量={}", results.size());
            dumpHtml(driver);

            return results;
        } catch (Exception e) {
            log.error("Selenium 以图搜图失败, 当前url={}", safeCurrentUrl(driver), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "以图搜图失败");
        } finally {
            driver.quit();
        }
    }

    /**
     * 指定 msedgedriver 路径。
     * 优先用项目下 drivers/msedgedriver.exe(手动下载, 绕过 Selenium Manager 的联网下载);
     * 找不到则回退 Selenium Manager 自动下载。
     */
    private static void setupDriverPath() {
        File local = new File("drivers" + File.separator + "msedgedriver.exe");
        if (local.exists()) {
            System.setProperty("webdriver.edge.driver", local.getAbsolutePath());
            log.info("[driver] 使用本地 msedgedriver: {}", local.getAbsolutePath());
        } else {
            log.warn("[driver] 未找到 drivers/msedgedriver.exe, 回退 Selenium Manager 自动下载(需联网)");
        }
    }

    private static EdgeOptions buildEdgeOptions() {
        EdgeOptions options = new EdgeOptions();
        // 原型阶段用【有头模式】方便观察; 部署到服务器时取消下一行注释改为无头
        // options.addArguments("--headless=new");
        options.addArguments(
                "--disable-blink-features=AutomationControlled",
                "--no-first-run",
                "--disable-extensions",
                "--window-size=1280,800"
        );
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        return options;
    }

    /**
     * 通过 CDP 在每个新文档加载前注入脚本, 抹掉 navigator.webdriver 等自动化特征
     */
    private static void injectAntiDetection(WebDriver driver) {
        try {
            String script = "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});"
                    + "window.chrome = {runtime: {}};"
                    + "Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN','zh','en']});"
                    + "Object.defineProperty(navigator, 'plugins', {get: () => [1,2,3,4,5]});";
            ((ChromiumDriver) driver).executeCdpCommand(
                    "Page.addScriptToEvaluateOnNewDocument", Map.of("source", script));
        } catch (Exception e) {
            log.warn("注入反检测脚本失败(可能影响识别): {}", e.getMessage());
        }
    }

    /**
     * 宽松策略: 取所有 img 过滤图标/logo/base64, 拿到真实 DOM 后再换精确 selector
     */
    private static List<ImageSearchResult> extractImages(WebDriver driver) {
        List<WebElement> imgs = driver.findElements(By.tagName("img"));
        List<ImageSearchResult> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (WebElement img : imgs) {
            String src = img.getAttribute("src");
            if (src == null || src.isBlank()) continue;
            if (src.startsWith("data:")) continue;
            String lower = src.toLowerCase();
            if (lower.contains("logo") || lower.contains("icon") || lower.contains(".svg")) continue;
            // 过滤百度识图的查询图(用户上传图的临时存储, shitu-query/bcebos 域名 + authorization 签名)
            if (lower.contains("bcebos.com") || lower.contains("shitu-query") || lower.contains("authorization=")) continue;
            if (!seen.add(src)) continue;
            ImageSearchResult r = new ImageSearchResult();
            r.setThumbUrl(src);
            results.add(r);
        }
        return results;
    }

    /**
     * 把结果页完整 HTML 写到临时文件, 便于据此校准精确 selector
     */
    private static void dumpHtml(WebDriver driver) {
        try {
            String html = driver.getPageSource();
            Path out = Paths.get(System.getProperty("java.io.tmpdir"), "baidu_search_result.html");
            Files.writeString(out, html);
            log.info("[debug] 结果页完整 HTML 已保存: {}", out);
        } catch (Exception e) {
            log.warn("保存 HTML 失败: {}", e.getMessage());
        }
    }

    private static void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String safeCurrentUrl(WebDriver driver) {
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            return "(获取url失败)";
        }
    }

    public static void main(String[] args) {
        // TODO 改成你本机一张图片的【绝对路径】
        String imagePath = "C:\\Users\\86187\\Pictures\\YangMi.jpg";
        List<ImageSearchResult> results = searchSimilarImages(imagePath);
        System.out.println("搜索成功, 相似图数量: " + results.size());
        results.forEach(r -> System.out.println("缩略图: " + r.getThumbUrl()));
    }
}
