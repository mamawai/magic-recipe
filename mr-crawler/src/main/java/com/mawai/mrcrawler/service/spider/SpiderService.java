package com.mawai.mrcrawler.service.spider;

import com.mawai.mrcrawler.model.*;

import java.util.Map;

/**
 * 爬虫服务接口
 */
public interface SpiderService {

    /**
     * 获取分类列表
     * @return 分类列表结果及状态
     */
    ApiResponse<Category> getCategories();

    /**
     * 获取分类食谱列表
     * @param category 分类
     * @param page 页码
     * @return 食谱列表和分页信息
     */
    ApiResponse<PageAndRecipes> getCategoryRecipes(String category, int page);

    /**
     * 搜索食谱
     * @param keyword 关键词
     * @return 搜索结果和分页信息
     */
    ApiResponse<PageAndRecipes> searchRecipes(String keyword);

    /**
     * 获取食谱详情
     * @param recipeNo 食谱编号
     * @return 食谱详情
     */
    ApiResponse<RecipeContent> getRecipeContent(String recipeNo);

} 