package com.mawai.mrcrawler.model;

import lombok.Data;

@Data
public class Page {
    private String next;
    private String prev;
    private int currentPage;
}