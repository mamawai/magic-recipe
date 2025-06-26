package com.mawai.mrcrawler.model;

import lombok.Data;

import java.util.List;

@Data
public class Category {
    private List<CategoryGroup> categories;
}
