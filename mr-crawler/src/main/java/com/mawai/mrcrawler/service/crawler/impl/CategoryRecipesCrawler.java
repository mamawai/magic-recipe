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

            if (categoryRecipes.getRecipes() == null || categoryRecipes.getRecipes().isEmpty() || categoryRecipes.getPage() == null) {
                log.warn("Failed to get recipes, possible anti-crawling mechanism detected");
                return null;
            }

            return categoryRecipes;
        } catch (Exception e) {
            log.error("Error crawling category recipes", e);
            return null;
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
