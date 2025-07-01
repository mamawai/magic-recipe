package com.mawai.mrcrawler.service.crawler.impl;

import com.mawai.mrcrawler.model.Category;
import com.mawai.mrcrawler.service.crawler.WebCrawler;
import com.mawai.mrcrawler.service.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CategoryCrawler extends WebCrawler<Category> {

    @Override
    public Category crawl(String args) {
        try {
            // 构建分类页面URL
            String categoryURL = siteConfig.getBaseUrl() + "/category/";
            log.info("Crawling categories from: {}", categoryURL);

            // 获取分类页面内容
            Document document = getDocument(categoryURL);

            // 解析分类列表
            Category category = categoryParser.parse(document);

            // 检查是否获取到有效数据
            if (category == null || category.getCategories() == null || category.getCategories().isEmpty()) {
                log.warn("Failed to get categories, possible anti-crawling mechanism detected");
                return null;
            }

            return category;

        } catch (Exception e) {
            log.error("Error crawling categories", e);
            return null;
        }
    }

    @Override
    public <R> void saveToDb(R category, String args) {
        try {
            dbCacheService.saveCategories((Category) category);
            log.info("已保存分类到数据库: {}", args);
        } catch (Exception e) {
            log.error("保存分类到数据库失败: {}", args, e);
        }
    }

    /**
     * 获取分类解析器
     * @return 分类解析器
     */
    public Parser<Category> getCategoryParser() {
        return categoryParser;
    }
}
