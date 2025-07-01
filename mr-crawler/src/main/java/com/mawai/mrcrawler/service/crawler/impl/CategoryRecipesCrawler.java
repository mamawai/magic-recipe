package com.mawai.mrcrawler.service.crawler.impl;

import com.mawai.mrcrawler.model.PageAndRecipes;
import com.mawai.mrcrawler.service.crawler.WebCrawler;
import com.mawai.mrcrawler.service.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class CategoryRecipesCrawler extends WebCrawler<PageAndRecipes> {

    /**
     * 爬取指定分类下的食谱列表（带缓存）
     * @param url 分类URL
     */
    @Override
    public PageAndRecipes crawl(String url) throws IOException {
        try {
            // 获取页面内容
            Document document = getDocument(url);

            PageAndRecipes categoryRecipes = pageAndRecipesParser.parse(document);

            if (categoryRecipes == null || categoryRecipes.getRecipes() == null || categoryRecipes.getRecipes().isEmpty() || categoryRecipes.getPage() == null) {
                log.warn("Failed to get recipes, possible anti-crawling mechanism detected");
                return null;
            }

            return categoryRecipes;
        } catch (Exception e) {
            log.error("Error crawling category recipes", e);
            return null;
        }
    }

    @Override
    public <R> void saveToDb(R result, String args) {
        // 解析args /category/1/?page=1
        String category;
        int page = 1; // 默认页码
        
        try {
            // 提取分类名称
            int startIndex = args.indexOf("/category/") + 10;
            int endIndex = args.indexOf("/", startIndex);
            if (endIndex == -1) {
                endIndex = args.indexOf("?", startIndex);
                if (endIndex == -1) {
                    endIndex = args.length();
                }
            }
            category = args.substring(startIndex, endIndex);
            
            // 提取页码
            int pageIndex = args.indexOf("page=");
            if (pageIndex != -1) {
                pageIndex += 5; // "page="的长度
                page = Integer.parseInt(args.substring(pageIndex));
            }
            
            // 调用数据库服务保存数据
            dbCacheService.saveCategoryRecipes(category, page, (PageAndRecipes) result);
            log.info("已保存分类菜谱到数据库: 分类={}, 页码={}", category, page);
        } catch (Exception e) {
            log.error("保存分类菜谱到数据库失败: {}", args, e);
        }
    }

    /**
     * 获取分类解析器
     * @return 分类解析器
     */
    public Parser<PageAndRecipes> getCategoryRecipesParser() {
        return pageAndRecipesParser;
    }
}
