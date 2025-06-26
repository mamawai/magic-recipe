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
public class SearchRecipeCrawler extends WebCrawler<PageAndRecipes> {

    /**
     * 爬取搜索食谱（食材）（带缓存）
     * @return 食谱列表
     */
    @Override
    public PageAndRecipes crawl(String keyword) throws IOException {
        try {
            // 构建搜索页面URL
            String url = siteConfig.getBaseUrl() + "/search/?keyword=" + keyword + "&cat=1001";
            log.info("Crawling search result: {}", url);

            // 获取分类页面内容
            Document document = getDocument(url);

            PageAndRecipes searchRecipes = pageAndRecipesParser.parse(document);

            if (searchRecipes == null || searchRecipes.getRecipes().isEmpty() || searchRecipes.getPage() == null) {
                log.warn("Failed to get search results for {}, possible anti-crawling mechanism detected", keyword);
                return null;
            }

            return searchRecipes;
        } catch (Exception e) {
            log.error("Error crawling searchRecipes results: {}", keyword, e);
            return null;
        }
    }

    /**
     * 获取分类解析器
     * @return 分类解析器
     */
    public Parser<PageAndRecipes> getSearchRecipesParser() {
        return pageAndRecipesParser;
    }
}
