package com.mawai.mrcrawler.service.parser.impl;

import com.mawai.mrcrawler.core.CssSelector;
import com.mawai.mrcrawler.model.Page;
import com.mawai.mrcrawler.service.parser.Parser;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PageParser implements Parser<Page> {

    private final CssSelector nextLinkSelector = new CssSelector("a.next", "href");
    private final CssSelector prevLinkSelector = new CssSelector("a.prev", "href");
    private final CssSelector prevPageSelector = new CssSelector("span.prev");
    private final CssSelector currentPageSelector = new CssSelector("span.now");
    private final CssSelector sourceSelector = new CssSelector("div.pager");

    /**
     * 解析分页信息
     * @param document HTML文档
     * @return 分页信息
     */
    public Page parse(Document document) {
        Page page = new Page();
        Elements elements = sourceSelector.select(document);
        if (elements.isEmpty()) return null;

        Element element = elements.first();
        if (element == null) return null;

        // 解析当前页码
        Elements currentPageElements = currentPageSelector.select(element);
        if (!currentPageElements.isEmpty()) {
            try {
                int currentPage = Integer.parseInt(Objects.requireNonNull(currentPageElements.first()).text().trim());
                page.setCurrentPage(currentPage);
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }

        // 解析下一页链接
        Elements nextElements = nextLinkSelector.select(element);
        if (!nextElements.isEmpty()) {
            page.setNext(nextLinkSelector.extract(nextElements.first()));
        }
        
        // 解析上一页链接 - 先检查是否有a.prev链接
        Elements prevLinkElements = prevLinkSelector.select(element);
        if (!prevLinkElements.isEmpty()) {
            page.setPrev(prevLinkSelector.extract(prevLinkElements.first()));
        } else {
            // 检查是否有span.prev元素，如果有表示当前是第一页
            Elements prevSpanElements = prevPageSelector.select(element);
            if (!prevSpanElements.isEmpty()) {
                // 当前是第一页，设置currentPage为1
                page.setCurrentPage(1);
            }
        }

        return page;
    }
}