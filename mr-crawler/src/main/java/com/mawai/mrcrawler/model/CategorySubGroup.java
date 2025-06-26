package com.mawai.mrcrawler.model;

import lombok.Data;

import java.util.List;

@Data
public class CategorySubGroup {
    private String name;
    private String link;
    private List<CategoryType> types;
}