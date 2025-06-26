package com.mawai.mrcrawler.service.parser.impl;

import com.mawai.mrcrawler.core.CssSelector;
import com.mawai.mrcrawler.model.Material;
import com.mawai.mrcrawler.model.RecipeContent;
import com.mawai.mrcrawler.model.Step;
import com.mawai.mrcrawler.service.parser.Parser;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 食谱详细内容解析器
 */
@Service
@RequiredArgsConstructor
public class ContentParser implements Parser<RecipeContent> {

    // CSS选择器常量
    private static final String NAME_SELECTOR = "h1.page-title";
    private static final String COVER_SELECTOR = "div.recipe-show > div.cover > img";
    private static final String COVER_ATTR = "src";
    private static final String GRADE_SELECTOR = "div.recipe-show > div.container > div.stats > div.score > span.number";
    private static final String MATERIALS_SELECTOR = "div.recipe-show > div.ings > table tr";
    private static final String STEPS_SELECTOR = "div.steps > ol li";
    private static final String STEPS_ATTR = "html";
    private static final String TIP_SELECTOR = "div.tip";
    private static final String SOURCE_SELECTOR = "div.main-panel";

    // 定义选择器
    private final CssSelector nameSelector = new CssSelector(NAME_SELECTOR);
    private final CssSelector coverSelector = new CssSelector(COVER_SELECTOR, COVER_ATTR);
    private final CssSelector gradeSelector = new CssSelector(GRADE_SELECTOR);
    private final CssSelector materialsSelector = new CssSelector(MATERIALS_SELECTOR);
    private final CssSelector stepsSelector = new CssSelector(STEPS_SELECTOR, STEPS_ATTR);
    private final CssSelector tipSelector = new CssSelector(TIP_SELECTOR);
    private final CssSelector sourceSelector = new CssSelector(SOURCE_SELECTOR);

    // 正则表达式，用于清理HTML标签
    private final Pattern pTagPattern = Pattern.compile("</?p[^>]*>");
    private final Pattern brTagPattern = Pattern.compile("<br\\s*?/?>");

    /**
     * 解析食谱内容
     * @param document HTML文档
     * @return 食谱内容
     */
    public RecipeContent parse(Document document) {
        RecipeContent content = new RecipeContent();
        Elements elements = sourceSelector.select(document);

        if (elements.isEmpty()) {
            return null;
        }

        // 设置名称
        Elements nameElements = nameSelector.select(document);
        if (!nameElements.isEmpty()) {
            content.setName(nameSelector.extract(nameElements.first()).trim());
        }

        // 设置封面图
        Elements coverElements = coverSelector.select(document);
        if (!coverElements.isEmpty()) {
            content.setCover(coverSelector.extract(coverElements.first()));
        }

        // 设置评分
        Elements gradeElements = gradeSelector.select(document);
        if (!gradeElements.isEmpty()) {
            content.setGrade(gradeSelector.extract(gradeElements.first()));
        }

        // 设置材料
        Elements materialElements = materialsSelector.select(document);
        List<Material> materials = new ArrayList<>();

        for (Element element : materialElements) {
            String name = "";
            String unit = "";

            Element nameEl = element.select("td.name").first();
            if (nameEl != null) {
                name = nameEl.text().trim();
                if (StringUtils.isBlank(name)) {
                    Element nameLink = nameEl.select("a").first();
                    if (nameLink != null) {
                        name = nameLink.text().trim();
                    }
                }
            }

            Element unitEl = element.select("td.unit").first();
            if (unitEl != null) {
                unit = unitEl.text().trim();
            }

            materials.add(new Material(name, unit));
        }
        content.setMaterials(materials);

        // 设置步骤
        Elements stepElements = stepsSelector.select(document);
        List<Step> steps = new ArrayList<>();

        for (int i = 0; i < stepElements.size(); i++) {
            Element element = stepElements.get(i);
            String desc = "";
            String img = "";

            // 清理HTML标签
            Element descEl = element.select("p").first();
            if (descEl != null) {
                desc = brTagPattern.matcher(pTagPattern.matcher(descEl.outerHtml()).replaceAll("")).replaceAll("\n").trim();
            }

            // 获取图片
            Element imgEl = element.select("img").first();
            if (imgEl != null) {
                img = imgEl.attr("src");
            }

            steps.add(new Step(i + 1, desc, img));
        }
        content.setSteps(steps);

        // 设置提示
        Elements tipElements = tipSelector.select(document);
        if (!tipElements.isEmpty()) {
            content.setTip(tipSelector.extract(tipElements.first()).trim());
        }

        return content;
    }
}