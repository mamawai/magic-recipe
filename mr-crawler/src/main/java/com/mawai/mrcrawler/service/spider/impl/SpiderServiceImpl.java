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

    // CATEGORY
    private static final String CATEGORY_RETRY_TASK_KEY = "crawler:retry:task:categories";
    private static final String CATEGORY_CACHE_KEY = "crawler:categories";
    private static final String CATEGORY_LOCK_KEY = "crawler:lock:categories";

    // RECIPE_CONTENT
    private static String RECIPE_CONTENT_RETRY_TASK_KEY = "crawler:retry:task:recipeContent";
    private static String RECIPE_CONTENT_CACHE_KEY = "crawler:recipeContent";
    private static String RECIPE_CONTENT_LOCK_KEY = "crawler:lock:recipeContent";

    // Category_Recipes
    private static String CATEGORY_RECIPES_RETRY_TASK_KEY = "crawler:retry:task:categoryRecipes";
    private static String CATEGORY_RECIPES_CACHE_KEY = "crawler:categoryRecipes";
    private static String CATEGORY_RECIPES_LOCK_KEY = "crawler:lock:categoryRecipes";

    // Search_Recipes
    private static String SEARCH_RECIPES_RETRY_TASK_KEY = "crawler:retry:task:searchRecipes";
    private static String SEARCH_RECIPES_CACHE_KEY = "crawler:searchRecipes";
    private static String SEARCH_RECIPES_LOCK_KEY = "crawler:lock:searchRecipes";

    private static final int LOCK_EXPIRE_SECONDS = 30;

    @Override
    public ApiResponse<Category> getCategories() {
        try {
            // 1. 先检查Redis缓存
            Category cachedCategory = cacheService.get(CATEGORY_CACHE_KEY);
            if (cachedCategory != null) {
                log.info("Returning categories from Redis cache");
                return ApiResponse.success(cachedCategory);
            }

            // 2. 检查当前是否有异步任务正在执行
            String taskStatus = cacheService.get(CATEGORY_RETRY_TASK_KEY);
            if (taskStatus != null) {
                log.info("Async Categories retry task is already running: {}", taskStatus);
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "Categories data is being fetched, please try again later"
                );
            }

            // 3. 尝试获取分布式锁
            String requestId = UUID.randomUUID().toString();
            boolean locked = cacheService.setIfAbsent(CATEGORY_LOCK_KEY, requestId, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

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
                            CATEGORY_RETRY_TASK_KEY,
                            CATEGORY_CACHE_KEY,
                            categoryCrawler.getCategoryParser()
                    );

                    return ApiResponse.error(
                            HttpStatus.NOT_FOUND.value(),
                            "Trying to bypass anti-crawling limitations, please try again later"
                    );
                } else {
                    // 6. 爬取成功，缓存结果
                    log.info("Successfully crawled categories, caching result");
                    cacheService.set(CATEGORY_CACHE_KEY, category, 12, TimeUnit.HOURS);

                    return ApiResponse.success(category);
                }
            } finally {
                // 7. 释放锁，但只释放自己的锁
                String currentValue = cacheService.get(CATEGORY_LOCK_KEY);
                if (requestId.equals(currentValue)) {
                    cacheService.delete(CATEGORY_LOCK_KEY);
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
            CATEGORY_RECIPES_RETRY_TASK_KEY = CATEGORY_RECIPES_RETRY_TASK_KEY + url;
            CATEGORY_RECIPES_CACHE_KEY = CATEGORY_RECIPES_CACHE_KEY + url;
            CATEGORY_RECIPES_LOCK_KEY = CATEGORY_RECIPES_LOCK_KEY + url;

            // 1. 先检查Redis缓存
            PageAndRecipes cachedCategoryRecipes = cacheService.get(CATEGORY_RECIPES_CACHE_KEY);
            if (cachedCategoryRecipes != null) {
                log.info("Returning categoryRecipes from Redis cache");
                return ApiResponse.success(cachedCategoryRecipes);
            }

            // 2. 检查当前是否有异步任务正在执行
            String taskStatus = cacheService.get(CATEGORY_RECIPES_RETRY_TASK_KEY);
            if (taskStatus != null) {
                log.info("Async CategoryRecipes retry task is already running: {}", taskStatus);
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "CategoryRecipes data is being fetched, please try again later"
                );
            }

            // 3. 尝试获取分布式锁
            String requestId = UUID.randomUUID().toString();
            boolean locked = cacheService.setIfAbsent(CATEGORY_RECIPES_LOCK_KEY, requestId, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

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
                            CATEGORY_RECIPES_RETRY_TASK_KEY,
                            CATEGORY_RECIPES_CACHE_KEY,
                            categoryRecipesCrawler.getCategoryRecipesParser()
                    );

                    return ApiResponse.error(
                            HttpStatus.NOT_FOUND.value(),
                            "Trying to bypass anti-crawling limitations, please try again later"
                    );
                } else {
                    // 6. 爬取成功，缓存结果
                    log.info("Successfully crawled categoryRecipes, caching result");
                    cacheService.set(CATEGORY_RECIPES_CACHE_KEY, categoryRecipes, 12, TimeUnit.HOURS);

                    return ApiResponse.success(categoryRecipes);
                }
            } finally {
                // 7. 释放锁，但只释放自己的锁
                String currentValue = cacheService.get(CATEGORY_RECIPES_LOCK_KEY);
                if (requestId.equals(currentValue)) {
                    cacheService.delete(CATEGORY_RECIPES_LOCK_KEY);
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
            String searchKey = keyword.trim();
            SEARCH_RECIPES_RETRY_TASK_KEY = SEARCH_RECIPES_RETRY_TASK_KEY + searchKey +":"+  page;
            SEARCH_RECIPES_CACHE_KEY = SEARCH_RECIPES_CACHE_KEY + searchKey +":"+  page;
            SEARCH_RECIPES_LOCK_KEY = SEARCH_RECIPES_LOCK_KEY + searchKey +":"+  page;

            if (page > 1) keyword = keyword + "&page=" + page;

            // 1. 先检查Redis缓存
            PageAndRecipes cachedSearchResults = cacheService.get(SEARCH_RECIPES_CACHE_KEY);
            if (cachedSearchResults != null) {
                log.info("Returning search results from Redis cache for keyword: {}", keyword);
                return ApiResponse.success(cachedSearchResults);
            }

            // 2. 检查当前是否有异步任务正在执行
            String taskStatus = cacheService.get(SEARCH_RECIPES_RETRY_TASK_KEY);
            if (taskStatus != null) {
                log.info("Async search recipes retry task is already running: {}", taskStatus);
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "Search data is being fetched, please try again later"
                );
            }

            // 3. 尝试获取分布式锁
            String requestId = UUID.randomUUID().toString();
            boolean locked = cacheService.setIfAbsent(SEARCH_RECIPES_LOCK_KEY, requestId, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

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
                            SEARCH_RECIPES_RETRY_TASK_KEY,
                            SEARCH_RECIPES_CACHE_KEY,
                            searchRecipeCrawler.getSearchRecipesParser()
                    );
                    return ApiResponse.error(
                            HttpStatus.NOT_FOUND.value(),
                            "Trying to bypass anti-crawling limitations, please try again later"
                    );
                } else {
                    // 6. 爬取成功，缓存结果
                    log.info("Successfully crawled search results for keyword: {}, caching result", keyword);
                    cacheService.set(SEARCH_RECIPES_CACHE_KEY, searchRecipes, 12, TimeUnit.HOURS);

                    return ApiResponse.success(searchRecipes);
                }
            } finally {
                // 7. 释放锁，但只释放自己的锁
                String currentValue = cacheService.get(SEARCH_RECIPES_LOCK_KEY);
                if (requestId.equals(currentValue)) {
                    cacheService.delete(SEARCH_RECIPES_LOCK_KEY);
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
            RECIPE_CONTENT_CACHE_KEY = RECIPE_CONTENT_CACHE_KEY + recipeNo;
            RECIPE_CONTENT_RETRY_TASK_KEY = RECIPE_CONTENT_RETRY_TASK_KEY + recipeNo;
            RECIPE_CONTENT_LOCK_KEY = RECIPE_CONTENT_LOCK_KEY + recipeNo;

            // 1. 先检查Redis缓存
            RecipeContent cachedRecipeContent = cacheService.get(RECIPE_CONTENT_CACHE_KEY);
            if (cachedRecipeContent != null) {
                log.info("Returning recipeContent from Redis cache");
                return ApiResponse.success(cachedRecipeContent);
            }

            // 2. 检查当前是否有异步任务正在执行
            String taskStatus = cacheService.get(RECIPE_CONTENT_RETRY_TASK_KEY);
            if (taskStatus != null) {
                log.info("Async RecipeContent retry task is already running: {}", taskStatus);
                return ApiResponse.error(
                        HttpStatus.ACCEPTED.value(),
                        "RecipeContent data is being fetched, please try again later"
                );
            }

            // 3. 尝试获取分布式锁
            String requestId = UUID.randomUUID().toString();
            boolean locked = cacheService.setIfAbsent(RECIPE_CONTENT_LOCK_KEY, requestId, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

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
                            RECIPE_CONTENT_RETRY_TASK_KEY,
                            RECIPE_CONTENT_CACHE_KEY,
                            recipeContentCrawler.getContentParser()
                    );

                    return ApiResponse.error(
                            HttpStatus.NOT_FOUND.value(),
                            "Trying to bypass anti-crawling limitations, please try again later"
                    );

                } else {
                    // 6. 爬取成功，缓存结果
                    log.info("Successfully crawled recipeContent, caching result");
                    cacheService.set(RECIPE_CONTENT_CACHE_KEY, content, 12, TimeUnit.HOURS);

                    return ApiResponse.success(content);
                }
            } finally {
                // 7. 释放锁，但只释放自己的锁
                String currentValue = cacheService.get(RECIPE_CONTENT_LOCK_KEY);
                if (requestId.equals(currentValue)) {
                    cacheService.delete(RECIPE_CONTENT_LOCK_KEY);
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
} 