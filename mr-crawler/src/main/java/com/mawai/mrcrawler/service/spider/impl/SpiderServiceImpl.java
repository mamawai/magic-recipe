package com.mawai.mrcrawler.service.spider.impl;

import com.mawai.mrcrawler.config.SiteConfig;
import com.mawai.mrcrawler.model.*;
import com.mawai.mrcrawler.service.spider.SpiderService;
import com.mawai.mrcrawler.service.cache.CacheService;
import com.mawai.mrcrawler.service.crawler.impl.CategoryCrawler;
import com.mawai.mrcrawler.service.crawler.impl.CategoryRecipesCrawler;
import com.mawai.mrcrawler.service.crawler.impl.RecipeContentCrawler;
import com.mawai.mrcrawler.service.crawler.impl.SearchRecipeCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpiderServiceImpl implements SpiderService {

    private final SiteConfig siteConfig;
    private final CategoryCrawler categoryCrawler;
    private final SearchRecipeCrawler searchRecipeCrawler;
    private final RecipeContentCrawler recipeContentCrawler;
    private final CategoryRecipesCrawler categoryRecipesCrawler;
    private final CacheService cacheService;

    // redis key
    private static final String RETRY_TASK_KEY = "retryTask:crawler:";
    private static final String CACHE_KEY = "cache:crawler:";
    private static final String LOCK_KEY = "lock:crawler:";

    private static final int LOCK_EXPIRE_SECONDS = 30;

    @Override
    public ApiResponse<Category> getCategories() {
        try {
            // 1. 先检查Redis缓存 --> 移动到CacheAsideAspect切面类
            // 2. 检查当前是否有异步任务正在执行
            List<String> redisKeys = buildRedisKey("getCategories", new ArrayList<>());
            String cachedKey = redisKeys.get(0);
            String retryTaskKey = redisKeys.get(1);
            String lockKey = redisKeys.get(2);

            String taskStatus = cacheService.get(retryTaskKey);
            if (taskStatus != null) {
                log.info("Async Categories retry task is already running: {}", taskStatus);
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "Categories data is being fetched, please try again later"
                );
            }

            // 3. 尝试获取分布式锁
            String requestId = UUID.randomUUID().toString();
            boolean locked = cacheService.setIfAbsent(lockKey, requestId, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

            if (!locked) {
                log.info("Another request is processing categories, waiting for result");
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "Another request is processing categories data, please try again later"
                );
            }

            try {
                // 4. 获取到锁，开始爬取数据
                log.info("Lock acquired, crawling categories");
                Category category = categoryCrawler.crawl("");

                if (category == null) {
                    // 5. 爬取失败，启动异步重试任务 但不等待结果
                    categoryCrawler.scheduleRetryCrawl(
                            0,
                            "/category/",
                            retryTaskKey,
                            cachedKey,
                            categoryCrawler.getCategoryParser()
                    );

                    return ApiResponse.error(
                            HttpStatus.NOT_FOUND.value(),
                            "Trying to bypass anti-crawling limitations, please try again later"
                    );
                } else {
                    // 6. 爬取成功，返回缓存结果 --> 移动到CacheAsideAspect切面类
                    return ApiResponse.success(category);
                }
            } finally {
                // 7. 释放锁，但只释放自己的锁
                String currentValue = cacheService.get(lockKey);
                if (requestId.equals(currentValue)) {
                    cacheService.delete(lockKey);
                    log.info("Lock released for categories request: {}", requestId);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching categories", e);
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage()
            );
        }
    }

    @Override
    public ApiResponse<PageAndRecipes> getCategoryRecipes(String category, int page) {
        try {
            String url = siteConfig.getBaseUrl() + "/category/" + category + "/";
            if (page > 1) url += "?page=" + page;

            ArrayList<Object> args = new ArrayList<>();
            args.add(category);
            args.add(page);
            List<String> redisKeys = buildRedisKey("getCategoryRecipes", args);
            String cachedKey = redisKeys.get(0);
            String retryTaskKey = redisKeys.get(1);
            String lockKey = redisKeys.get(2);

            // 1. 先检查Redis缓存 --> 移动到CacheAsideAspect切面类
            // 2. 检查当前是否有异步任务正在执行
            String taskStatus = cacheService.get(retryTaskKey);
            if (taskStatus != null) {
                log.info("Async CategoryRecipes retry task is already running: {}", taskStatus);
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "CategoryRecipes data is being fetched, please try again later"
                );
            }

            // 3. 尝试获取分布式锁
            String requestId = UUID.randomUUID().toString();
            boolean locked = cacheService.setIfAbsent(lockKey, requestId, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

            if (!locked) {
                log.info("Another request is processing categoryRecipes, waiting for result");
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "Another request is processing categoryRecipes data, please try again later"
                );
            }

            try {
                // 4. 获取到锁，开始爬取数据
                log.info("Lock acquired, crawling categoryRecipes");
                PageAndRecipes categoryRecipes = categoryRecipesCrawler.crawl(url);

                if (categoryRecipes == null) {
                    // 5. 爬取失败，启动异步重试任务 但不等待结果
                    categoryRecipesCrawler.scheduleRetryCrawl(
                            0,
                            "/category/" + category + "/" + (page > 1 ? "?page=" + page : ""),
                            retryTaskKey,
                            cachedKey,
                            categoryRecipesCrawler.getCategoryRecipesParser()
                    );

                    return ApiResponse.error(
                            HttpStatus.NOT_FOUND.value(),
                            "Trying to bypass anti-crawling limitations, please try again later"
                    );
                } else {
                    // 6. 爬取成功，缓存结果 --> 移动到CacheAsideAspect切面类
                    return ApiResponse.success(categoryRecipes);
                }
            } finally {
                // 7. 释放锁，但只释放自己的锁
                String currentValue = cacheService.get(lockKey);
                if (requestId.equals(currentValue)) {
                    cacheService.delete(lockKey);
                    log.info("Lock released for categoryRecipes request: {}", requestId);
                }
            }
        } catch (IOException e) {
            log.error("Error fetching categoryRecipes", e);
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage()
            );
        }
    }

    @Override
    public ApiResponse<PageAndRecipes> searchRecipes(String keyword, int page) {
        try {
            if (page > 1) keyword = keyword.trim() + "&page=" + page;

            ArrayList<Object> args = new ArrayList<>();
            args.add(keyword);
            args.add(page);
            List<String> redisKeys = buildRedisKey("searchRecipes", args);
            String cachedKey = redisKeys.get(0);
            String retryTaskKey = redisKeys.get(1);
            String lockKey = redisKeys.get(2);

            // 1. 先检查Redis缓存 --> 移动到CacheAsideAspect切面类
            // 2. 检查当前是否有异步任务正在执行
            String taskStatus = cacheService.get(retryTaskKey);
            if (taskStatus != null) {
                log.info("Async search recipes retry task is already running: {}", taskStatus);
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "Search data is being fetched, please try again later"
                );
            }

            // 3. 尝试获取分布式锁
            String requestId = UUID.randomUUID().toString();
            boolean locked = cacheService.setIfAbsent(lockKey, requestId, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

            if (!locked) {
                log.info("Another request is processing search results for keyword: {}, waiting for result", keyword);
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "Another request is processing search keyword, please try again later"
                );
            }

            try {
                // 4. 获取到锁，开始爬取数据
                log.info("Lock acquired, crawling search results for keyword: {}", keyword);
                PageAndRecipes searchRecipes = searchRecipeCrawler.crawl(keyword);

                if (searchRecipes == null) {
                    // 5. 爬取失败，启动异步重试任务 但不等待结果
                    searchRecipeCrawler.scheduleRetryCrawl(
                            0,
                            "/search/?keyword=" + keyword + "&cat=1001",
                            retryTaskKey,
                            cachedKey,
                            searchRecipeCrawler.getSearchRecipesParser()
                    );
                    return ApiResponse.error(
                            HttpStatus.NOT_FOUND.value(),
                            "Trying to bypass anti-crawling limitations, please try again later"
                    );
                } else {
                    // 6. 爬取成功，缓存结果 --> 移动到CacheAsideAspect切面类
                    return ApiResponse.success(searchRecipes);
                }
            } finally {
                // 7. 释放锁，但只释放自己的锁
                String currentValue = cacheService.get(lockKey);
                if (requestId.equals(currentValue)) {
                    cacheService.delete(lockKey);
                    log.info("Lock released for searchRecipes request: {}", requestId);
                }
            }
        } catch (Exception e) {
            log.error("Error searching recipes for keyword: {}", keyword, e);
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage()
            );
        }
    }

    @Override
    public ApiResponse<RecipeContent> getRecipeContent(String recipeNo) {
        try {
            ArrayList<Object> args = new ArrayList<>();
            args.add(recipeNo);
            List<String> redisKeys = buildRedisKey("getRecipeContent", args);
            String cachedKey = redisKeys.get(0);
            String retryTaskKey = redisKeys.get(1);
            String lockKey = redisKeys.get(2);


            // 1. 先检查Redis缓存 --> 移动到CacheAsideAspect切面类
            // 2. 检查当前是否有异步任务正在执行
            String taskStatus = cacheService.get(retryTaskKey);
            if (taskStatus != null) {
                log.info("Async RecipeContent retry task is already running: {}", taskStatus);
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "RecipeContent data is being fetched, please try again later"
                );
            }

            // 3. 尝试获取分布式锁
            String requestId = UUID.randomUUID().toString();
            boolean locked = cacheService.setIfAbsent(lockKey, requestId, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

            if (!locked) {
                log.info("Another request is processing recipeContent, waiting for result");
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "Another request is processing recipeContent data, please try again later"
                );
            }
            try {
                // 4. 获取到锁，开始爬取数据
                log.info("Lock acquired, crawling recipeContent");
                RecipeContent content = recipeContentCrawler.crawl(recipeNo);

                if (content == null) {
                    // 5. 爬取失败，启动异步重试任务 但不等待结果
                    recipeContentCrawler.scheduleRetryCrawl(
                            0,
                            "/recipe/" + recipeNo + "/",
                            retryTaskKey,
                            cachedKey,
                            recipeContentCrawler.getContentParser()
                    );

                    return ApiResponse.error(
                            HttpStatus.NOT_FOUND.value(),
                            "Trying to bypass anti-crawling limitations, please try again later"
                    );

                } else {
                    // 6. 爬取成功，缓存结果 --> 移动到CacheAsideAspect切面类
                    return ApiResponse.success(content);
                }
            } finally {
                // 7. 释放锁，但只释放自己的锁
                String currentValue = cacheService.get(lockKey);
                if (requestId.equals(currentValue)) {
                    cacheService.delete(lockKey);
                    log.info("Lock released for recipeContent request: {}", requestId);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching recipeContent", e);
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage()
            );
        }
    }

    /**
     * 构建Redis缓存的key
     */
    public List<String> buildRedisKey(String methodName, List<Object> args) {
        StringBuilder cacheSb = new StringBuilder(CACHE_KEY).append(methodName);
        StringBuilder retryTaskSb = new StringBuilder(RETRY_TASK_KEY).append(methodName);
        StringBuilder lockSb = new StringBuilder(LOCK_KEY).append(methodName);
        for (Object arg : args) {
            cacheSb.append(":").append(arg);
            retryTaskSb.append(":").append(arg);
            lockSb.append(":").append(arg);
        }
        return Arrays.asList(cacheSb.toString(), retryTaskSb.toString(), lockSb.toString());
    }
} 