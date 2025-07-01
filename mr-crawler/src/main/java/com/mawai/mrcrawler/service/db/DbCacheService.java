package com.mawai.mrcrawler.service.db;

import com.mawai.mrcrawler.model.Category;
import com.mawai.mrcrawler.model.PageAndRecipes;
import com.mawai.mrcrawler.model.RecipeContent;

/**
 * 数据库缓存服务接口
 * 使用现有实体类实现旁路缓存
 */
public interface DbCacheService {
    
    /**
     * 从数据库获取分类数据
     * @return 分类数据
     */
    Category getCategories();
    
    /**
     * 保存分类数据到数据库
     * @param category 分类数据
     */
    void saveCategories(Category category);
    
    /**
     * 从数据库获取分类食谱列表
     * @param category 分类名称
     * @param page 页码
     * @return 食谱列表和分页信息
     */
    PageAndRecipes getCategoryRecipes(String category, int page);
    
    /**
     * 保存分类食谱列表到数据库
     * @param category 分类名称
     * @param page 页码
     * @param pageAndRecipes 食谱列表和分页信息
     */
    void saveCategoryRecipes(String category, int page, PageAndRecipes pageAndRecipes);
    
    /**
     * 从数据库获取搜索结果
     * @param keyword 搜索关键词
     * @param page 页码
     * @return 食谱列表和分页信息
     */
    PageAndRecipes getSearchRecipes(String keyword, int page);
    
    /**
     * 保存搜索结果到数据库
     * @param keyword 搜索关键词
     * @param page 页码
     * @param pageAndRecipes 食谱列表和分页信息
     */
    void saveSearchRecipes(String keyword, int page, PageAndRecipes pageAndRecipes);
    
    /**
     * 从数据库获取食谱详情
     * @param recipeNo 食谱编号
     * @return 食谱详情
     */
    RecipeContent getRecipeContent(String recipeNo);
    
    /**
     * 保存食谱详情到数据库
     * @param recipeNo 食谱编号
     * @param recipeContent 食谱详情
     */
    void saveRecipeContent(String recipeNo, RecipeContent recipeContent);
} 