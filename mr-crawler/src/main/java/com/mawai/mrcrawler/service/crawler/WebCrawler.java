package com.mawai.mrcrawler.service.crawler;

import com.mawai.mrcrawler.config.SiteConfig;
import com.mawai.mrcrawler.model.*;
import com.mawai.mrcrawler.service.cache.CacheService;
import com.mawai.mrcrawler.service.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class WebCrawler<T> {

    // 网络客户端实例
    @Autowired
    protected OkHttpClient okHttpClient;
    // 缓存服务
    @Autowired
    protected CacheService cacheService;
    // 站点配置实例
    @Autowired
    protected SiteConfig siteConfig;
    // 解析器实例
    @Autowired
    protected Parser<Category> categoryParser;
    @Autowired
    protected Parser<RecipeContent> contentParser;
    @Autowired
    protected Parser<PageAndRecipes> pageAndRecipesParser;

    // Redis键值定义
    protected static final String SEARCH_RECIPES_CACHE_KEY = "crawler:search:";
    protected static final int MAX_RETRY_COUNT = 5;

    /**
     * 爬虫主方法，根据URL爬取数据并返回结果
     * @param args 传参字符串
     * @return 爬取结果
     */
    public abstract T crawl(String args) throws IOException;

    /**
     * 获取网页内容并解析为Document
     * @param url 网页URL
     * @return Jsoup Document对象
     */
    public Document getDocument(String url) throws IOException {
        log.info("Fetching webpage: {}", url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }

            if (response.body() == null) {
                throw new IOException("Empty response body");
            }

            String html = response.body().string();
            return Jsoup.parse(html, url);
        } catch (Exception e) {
            log.error("Error fetching webpage: {}", url, e);
            throw e;
        }
    }

    /**
     * 异步重试爬取
     * execute and forget
     * @param retryCount 当前重试次数
     * @param args 爬取参数
     * @param retryTaskKey Redis任务键
     * @param cacheKey Redis缓存键
     * @param parser 解析器
     */
    @Async
    public <R> void scheduleRetryCrawl(int retryCount, String args, String retryTaskKey, String cacheKey, Parser<R> parser) {
        // 检查重试次数
        if (retryCount >= MAX_RETRY_COUNT) {
            log.error("Maximum retry count reached, failed to crawl");
            cacheService.delete(retryTaskKey);
            return;
        }
        
        // 设置任务状态
        cacheService.setStatus(retryTaskKey, "CRAWLING", 30, TimeUnit.MINUTES);
        
        try {
            // 根据重试次数增加延迟时间（指数退避）
            int delaySeconds = (int) Math.pow(2, retryCount) * 5;
            // 添加随机抖动，避免固定间隔请求
            delaySeconds += ThreadLocalRandom.current().nextInt(3);
            
            log.info("Scheduled retry #{} to crawl in {} seconds", retryCount + 1, delaySeconds);
            
            // 等待延迟时间
            Thread.sleep(delaySeconds * 1000L);
            
            // 爬取数据
            String url = siteConfig.getBaseUrl() + args;
            
            log.info("Attempting to crawl: {}", url);
            try {
                Document document = getDocument(url);
                // 使用传入的parser进行解析，得到对应类型的结果
                R result = parser.parse(document);
                
                if (result != null) {
                    log.info("Successfully crawled data after retry #{}", retryCount + 1);
                    // 更新缓存
                    cacheService.set(cacheKey, result, 12, TimeUnit.HOURS);
                    // 删除任务标记
                    cacheService.delete(retryTaskKey);
                } else {
                    log.warn("Failed to get data, will retry");
                    // 递归继续重试
                    scheduleRetryCrawl(retryCount + 1, args, retryTaskKey, cacheKey, parser);
                }
            } catch (Exception e) {
                log.error("Error while getting data", e);
                // 递归继续重试
                scheduleRetryCrawl(retryCount + 1, args, retryTaskKey, cacheKey, parser);
            }
            
        } catch (Exception e) {
            log.error("Error during retry #{} to crawl", retryCount + 1, e);
            // 递归继续重试
            scheduleRetryCrawl(retryCount + 1, args, retryTaskKey, cacheKey, parser);
        }
    }
}
