package com.mawai.mrcrawler.service.parser.impl;

import com.mawai.mrcrawler.core.CssSelector;
import com.mawai.mrcrawler.model.Category;
import com.mawai.mrcrawler.model.CategoryGroup;
import com.mawai.mrcrawler.model.CategorySubGroup;
import com.mawai.mrcrawler.model.CategoryType;
import com.mawai.mrcrawler.service.parser.Parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryParser implements Parser<Category> {

    private final CssSelector categoriesSelector = new CssSelector("div.cates-list");
    private final CssSelector sourceSelector = new CssSelector("div.category-container > div");

    /**
     * 解析分类信息
     * @param document HTML文档
     * @return 分类信息
     */
    public Category parse(Document document) {
        Category category = new Category();
        Elements sourceElements = sourceSelector.select(document);

        if (sourceElements.isEmpty()) {
            log.error("No source elements found");
            return null;
        }

        List<CategoryGroup> categoryGroups = new ArrayList<>();

        for (Element element : sourceElements) {
            Elements categoryElements = categoriesSelector.select(element);

            for (Element categoryElement : categoryElements) {
                CategoryGroup group = new CategoryGroup();

                // 获取分类名称
                Element titleElement = categoryElement.selectFirst("div > h3");
                if (titleElement != null) {
                    group.setName(titleElement.text().trim());
                }

                // 获取分类列表
                Element catesList = categoryElement.select("div").get(4);
                // 有的分类列表是5，有的是4
                if (catesList.childrenSize() < 3) catesList = categoryElement.select("div").get(5);
                if (catesList != null) {
                    List<CategorySubGroup> subGroups = new ArrayList<>();

                    // 获取所有h4标题和对应的ul列表
                    Elements h4Elements = catesList.select("h4");
                    Elements ulElements = catesList.select("ul");

                    for (int i = 0; i < h4Elements.size(); i++) {
                        CategorySubGroup subGroup = new CategorySubGroup();
                        Element h4 = h4Elements.get(i);

                        // 设置子分类名称
                        subGroup.setName(h4.text().trim());
                        // /category/12345/
                        String attr = h4.select("a").attr("href");
                        String[] split = attr.split("/");
                        subGroup.setLink(split.length > 2 ? split[2] : attr);

                        // 处理对应的ul中的类型
                        if (i < ulElements.size()) {
                            Element ul = ulElements.get(i);
                            Elements liElements = ul.select("li > a");

                            List<CategoryType> types = new ArrayList<>();
                            for (Element a : liElements) {
                                CategoryType type = new CategoryType();
                                type.setName(a.text().trim());
                                String attr1 = a.attr("href");
                                String[] split1 = attr1.split("/");
                                type.setLink(split1.length > 2 ? split1[2] : attr1);
                                types.add(type);
                            }

                            subGroup.setTypes(types);
                        } else {
                            subGroup.setTypes(new ArrayList<>());
                        }

                        subGroups.add(subGroup);
                    }

                    group.setList(subGroups);
                }

                categoryGroups.add(group);
            }
        }

        category.setCategories(categoryGroups);
        return category;
    }
}
