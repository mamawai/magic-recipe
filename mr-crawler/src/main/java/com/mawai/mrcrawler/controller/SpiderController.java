package com.mawai.mrcrawler.controller;

import com.mawai.mrcrawler.model.*;
import com.mawai.mrcrawler.model.PageAndRecipes;
import com.mawai.mrcrawler.service.spider.SpiderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/spider")
@RequiredArgsConstructor
public class SpiderController {

    private final SpiderService spiderService;

    /**
     * 获取分类列表
     */
    @Operation(summary = "获取分类列表", description = "获取所有分类列表")
    @GetMapping("/category")
    public ResponseEntity<ApiResponse<Category>> getCategories() {
        ApiResponse<Category> response = spiderService.getCategories();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 获取指定分类的食谱列表
     * @param cat 分类
     * @param page 页码
     */
    @Operation(summary = "获取指定分类的食谱列表", description = "获取指定分类的所有食谱列表")
    @GetMapping("/category/{cat}")
    public ResponseEntity<ApiResponse<PageAndRecipes>> getCategoryRecipes(
            @PathVariable String cat,
            @RequestParam(required = false, defaultValue = "1") int page) {
        
        ApiResponse<PageAndRecipes> response = spiderService.getCategoryRecipes(cat, page);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 搜索食谱
     * @param keyword 关键词
     */
    @Operation(summary = "搜索食谱", description = "搜索指定关键词的食谱列表")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageAndRecipes>> searchRecipes(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "1") int page) {

        ApiResponse<PageAndRecipes> response = spiderService.searchRecipes(keyword, page);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 获取食谱详情
     * @param no 食谱ID
     */
    @Operation(summary = "获取食谱详情", description = "获取指定ID的食谱详情")
    @GetMapping("/recipe/{no}")
    public ResponseEntity<ApiResponse<RecipeContent>> getRecipeContent(@PathVariable String no) {
        ApiResponse<RecipeContent> response = spiderService.getRecipeContent(no);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
