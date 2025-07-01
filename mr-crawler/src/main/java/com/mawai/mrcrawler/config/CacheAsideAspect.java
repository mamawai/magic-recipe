package com.mawai.mrcrawler.config;

import com.mawai.mrcrawler.model.*;
import com.mawai.mrcrawler.service.cache.CacheService;
import com.mawai.mrcrawler.service.db.DbCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 旁路缓存（Cache-Aside）模式实现
 * 1. 先从Redis缓存中获取数据
 * 2. Redis未命中时，从数据库获取数据
 * 3. 数据库中有数据时，更新Redis并返回
 * 4. 数据库也没有数据时，调用原始方法
 * 5. 原始方法返回数据后，同时更新Redis和数据库
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class CacheAsideAspect {
    
    private final CacheService cacheService;
    private final DbCacheService dbCacheService;

    private static final String CACHE_KEY = "cache:crawler:";
    
    /**
     * 定义切点：SpiderService的所有方法
     */
    @Pointcut("execution(* com.mawai.mrcrawler.service.spider.SpiderService.*(..))")
    public void spiderServiceMethods() {}
    
    /**
     * 环绕通知：实现旁路缓存逻辑
     */
    @Around("spiderServiceMethods()")
    public Object cacheAside(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法名和参数
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // 构建缓存键
        String cacheKey = buildCacheKey(methodName, args);
        log.info("拦截方法: {}, 缓存键: {}", methodName, cacheKey);
        
        // 1. 先从Redis缓存获取
        Object redisResult = getFromRedis(cacheKey);
        if (redisResult != null) {
            log.info("Redis缓存命中: {}", cacheKey);
            return ApiResponse.success(redisResult);
        }
        
        // 2. Redis未命中，从MySQL获取
        Object dbResult = getFromDb(methodName, args);
        if (dbResult != null) {
            log.info("MySQL查询命中: {}", cacheKey);
            // 更新Redis缓存
            updateRedisCache(cacheKey, dbResult);
            return ApiResponse.success(dbResult);
        }
        
        // 3. 都未命中，执行原始方法
        log.info("缓存未命中，执行原始方法: {}", cacheKey);
        ApiResponse<?> response = (ApiResponse<?>) joinPoint.proceed();
        
        // 4. 原始方法执行成功，更新缓存
        if (response.getStatus() == HttpStatus.OK.value() && response.getData() != null) {
            Object data = response.getData();
            // 异步更新MySQL和Redis缓存
            updateRedisCache(cacheKey, data);
            saveToDb(methodName, args, data);
            log.info("更新MySQL和Redis缓存: {}", cacheKey);
        }
        
        return response;
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(String methodName, Object[] args) {
        StringBuilder key = new StringBuilder(CACHE_KEY).append(methodName);
        
        if (args != null) {
            for (Object arg : args) {
                if (arg != null) {
                    key.append(":").append(arg);
                }
            }
        }
        
        return key.toString();
    }
    
    /**
     * 从Redis获取数据
     */
    private Object getFromRedis(String cacheKey) {
        try {
            if (cacheService.hasKey(cacheKey)) {
                return cacheService.get(cacheKey);
            }
        } catch (Exception e) {
            log.error("从Redis获取数据失败: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * 更新Redis缓存
     */
    private void updateRedisCache(String cacheKey, Object data) {
        try {
            // 缓存过期时间为12小时
            cacheService.set(cacheKey, data, 12, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("更新Redis缓存失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从MySQL获取数据
     */
    private Object getFromDb(String methodName, Object[] args) {
        try {
            switch (methodName) {
                case "getCategories":
                    return dbCacheService.getCategories();
                case "getCategoryRecipes":
                    if (args.length >= 2) {
                        String category = (String) args[0];
                        int page = (int) args[1];
                        return dbCacheService.getCategoryRecipes(category, page);
                    }
                    break;
                case "searchRecipes":
                    if (args.length >= 2) {
                        String keyword = (String) args[0];
                        int page = (int) args[1];
                        return dbCacheService.getSearchRecipes(keyword, page);
                    }
                    break;
                case "getRecipeContent":
                    if (args.length >= 1) {
                        String recipeNo = (String) args[0];
                        return dbCacheService.getRecipeContent(recipeNo);
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("从MySQL获取数据失败: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * 保存数据到MySQL
     */
    private void saveToDb(String methodName, Object[] args, Object data) {
        try {
            switch (methodName) {
                case "getCategories":
                    dbCacheService.saveCategories((Category) data);
                    break;
                case "getCategoryRecipes":
                    if (args.length >= 2) {
                        String category = (String) args[0];
                        int page = (int) args[1];
                        dbCacheService.saveCategoryRecipes(category, page, (PageAndRecipes) data);
                    }
                    break;
                case "searchRecipes":
                    if (args.length >= 2) {
                        String keyword = (String) args[0];
                        int page = (int) args[1];
                        dbCacheService.saveSearchRecipes(keyword, page, (PageAndRecipes) data);
                    }
                    break;
                case "getRecipeContent":
                    if (args.length >= 1) {
                        String recipeNo = (String) args[0];
                        dbCacheService.saveRecipeContent(recipeNo, (RecipeContent) data);
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("保存数据到MySQL失败: {}", e.getMessage(), e);
        }
    }
} 