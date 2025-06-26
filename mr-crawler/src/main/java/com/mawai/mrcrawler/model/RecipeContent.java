package com.mawai.mrcrawler.model;

import lombok.Data;

import java.util.List;

@Data
public class RecipeContent {
    private String name;
    private String cover;
    private String grade;
    private List<Material> materials;
    private List<Step> steps;
    private String tip;
}
