package com.mawai.mrcrawler.model;

import lombok.Data;

import java.util.List;

@Data
public class CategoryGroup {
    private String name;
    private List<CategorySubGroup> list;
}