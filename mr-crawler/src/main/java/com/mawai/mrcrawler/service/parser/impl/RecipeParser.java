package com.mawai.mrcrawler.service.parser.impl;

import com.mawai.mrcrawler.core.CssSelector;
import com.mawai.mrcrawler.model.Recipe;
import com.mawai.mrcrawler.service.parser.Parser;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeParser implements Parser<List<Recipe>> {

    // CSS选择器常量
    private static final String RECIPE_LIST_SELECTOR = "div[class*=\"main-panel\"] div.normal-recipe-list ul.list li";
    private static final String URL_SELECTOR = "div.recipe > a";
    private static final String URL_ATTR = "href";
    private static final String NAME_SELECTOR = "div.recipe > div.info > p.name > a";
    private static final String COVER_SELECTOR = "div.recipe > a > div.cover > img";
    private static final String COVER_ATTR = "data-src";

    // 定义主选择器，用于获取食谱元素列表
    private final CssSelector recipeListSelector = new CssSelector(RECIPE_LIST_SELECTOR);

    /**
     * 从文档中解析食谱列表
     * @param document HTML文档
     * @return 食谱列表
     */
    public List<Recipe> parse(Document document) {
        List<Recipe> recipes = new ArrayList<>();
        Elements recipeElements = recipeListSelector.select(document);

        for (Element element : recipeElements) {
            Recipe recipe = new Recipe();
            
            // 获取URL - 直接查找元素并提取属性
            Element urlElement = element.selectFirst(URL_SELECTOR);
            if (urlElement != null) {
                String url = urlElement.attr(URL_ATTR);
                recipe.setUrl(StringUtils.isNotBlank(url) ? url : "");
            }

            // 获取名称 - 直接查找元素并提取文本
            Element nameElement = element.selectFirst(NAME_SELECTOR);
            if (nameElement != null) {
                String name = nameElement.text();
                recipe.setName(StringUtils.isNotBlank(name) ? name.trim() : "");
            }

            // 获取封面图 - 直接查找元素并提取属性
            Element coverElement = element.selectFirst(COVER_SELECTOR);
            if (coverElement != null) {
                String cover = coverElement.attr(COVER_ATTR);
                recipe.setCover(StringUtils.isNotBlank(cover) ? cover : "");
            }

            recipes.add(recipe);
        }

        return recipes.isEmpty() ? null : recipes;
    }
}
