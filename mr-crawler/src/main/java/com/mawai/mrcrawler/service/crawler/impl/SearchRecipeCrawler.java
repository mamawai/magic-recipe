package com.mawai.mrcrawler.service.crawler.impl;

import com.mawai.mrcrawler.model.PageAndRecipes;
import com.mawai.mrcrawler.service.crawler.WebCrawler;
import com.mawai.mrcrawler.service.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SearchRecipeCrawler extends WebCrawler<PageAndRecipes> {

    /**
     * 爬取搜索食谱（食材）（带缓存）
     * @return 食谱列表
     */
    @Override
    public PageAndRecipes crawl(String keyword) {
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

    @Override
    public <R> void saveToDb(R result, String args) {
        // /search/?keyword=白菜&page=3&cat=1001
        try {
            // 提取keyword参数
            String keyword = null;
            int keywordIndex = args.indexOf("keyword=");
            if (keywordIndex != -1) {
                keywordIndex += 8; // "keyword="的长度
                int endIndex = args.indexOf("&", keywordIndex);
                if (endIndex == -1) {
                    endIndex = args.length();
                }
                keyword = args.substring(keywordIndex, endIndex);
            }
            
            // 提取page参数，默认为1
            int page = 1;
            int pageIndex = args.indexOf("page=");
            if (pageIndex != -1) {
                pageIndex += 5; // "page="的长度
                int endIndex = args.indexOf("&", pageIndex);
                if (endIndex == -1) {
                    endIndex = args.length();
                }
                try {
                    page = Integer.parseInt(args.substring(pageIndex, endIndex));
                } catch (NumberFormatException e) {
                    log.warn("无法解析页码，使用默认值1: {}", args);
                }
            }
            
            // 调用数据库服务保存数据
            if (keyword != null) {
                dbCacheService.saveSearchRecipes(keyword, page, (PageAndRecipes) result);
                log.info("已保存搜索结果到数据库: keyword={}, page={}", keyword, page);
            } else {
                log.warn("无法从URL中提取搜索关键词: {}", args);
            }
        } catch (Exception e) {
            log.error("保存搜索结果到数据库失败: {}", args, e);
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
