package com.mawai.mrcrawler.service.crawler.impl;

import com.mawai.mrcrawler.model.RecipeContent;
import com.mawai.mrcrawler.service.crawler.WebCrawler;
import com.mawai.mrcrawler.service.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RecipeContentCrawler extends WebCrawler<RecipeContent> {

    /**
     * 爬取食谱详细内容（带缓存）
     * @param recipeNo 食谱编号
     * @return 食谱内容
     */
    @Override
    public RecipeContent crawl(String recipeNo) {
        try {
            // 构建食谱页面URL
            String url = siteConfig.getBaseUrl() + "/recipe/" + recipeNo + "/";
            log.info("Crawling recipe content: {}", url);

            // 获取食谱页面内容
            Document document = getDocument(url);

            // 解析食谱内容
            RecipeContent content = contentParser.parse(document);

            // 检查是否获取到有效数据
            if (content == null || content.getName() == null || content.getName().isEmpty()) {
                log.warn("Failed to get recipe content for {}, possible anti-crawling mechanism detected", recipeNo);
                return null;
            }
            return content;
        } catch (Exception e) {
            log.error("Error crawling recipe content: {}", recipeNo, e);
            return null;
        }
    }

    @Override
    public <R> void saveToDb(R result, String args) {
        // /recipe/123456/
        try {
            // 只处理格式为"/recipe/123456/"的URL
            String recipeNo = args.substring(args.indexOf("/recipe/") + 8, args.lastIndexOf("/"));
            dbCacheService.saveRecipeContent(recipeNo, (RecipeContent) result);
            log.info("已保存食谱内容到数据库: recipeNo={}", recipeNo);
        } catch (Exception e) {
            log.error("保存食谱内容到数据库失败: {}", args, e);
        }
    }

    /**
     * 获取分类解析器
     * @return 分类解析器
     */
    public Parser<RecipeContent> getContentParser() {
        return contentParser;
    }
}
