package com.mawai.mrcrawler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageAndRecipes {
    private Page page;
    private List<Recipe> recipes;
}
