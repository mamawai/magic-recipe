package com.mawai.mrcrawler.service.parser;

import org.jsoup.nodes.Document;

public interface Parser<T> {

    /**
     * 解析HTML文档
     * @param html HTML文档
     * @return 解析结果
     */
    T parse(Document html);
}
