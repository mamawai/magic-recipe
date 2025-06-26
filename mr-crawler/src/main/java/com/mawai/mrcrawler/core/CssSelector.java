package com.mawai.mrcrawler.core;


import lombok.Getter;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Getter
public class CssSelector implements Selector {
    private final String cssQuery;
    private final String attribute;

    public CssSelector(String cssQuery) {
        this(cssQuery, null);
    }

    public CssSelector(String cssQuery, String attribute) {
        this.cssQuery = cssQuery;
        this.attribute = attribute;
    }

    @Override
    public Elements select(Element element) {
        return element.select(cssQuery);
    }

    /**
     * 提取元素的文本或属性值
     * @param element 元素
     * @return 文本或属性值
     */
    public String extract(Element element) {
        if (attribute != null && !attribute.equals("html")) {
            return element.attr(attribute);
        } else if (attribute != null) {
            return element.html();
        } else {
            return element.text();
        }
    }
}
