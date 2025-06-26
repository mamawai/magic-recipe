package com.mawai.mrcrawler.core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public interface Selector {
    /**
     * 执行选择器并返回Elements
     * @param element DOM元素
     * @return 选择结果
     */
    Elements select(Element element);
}