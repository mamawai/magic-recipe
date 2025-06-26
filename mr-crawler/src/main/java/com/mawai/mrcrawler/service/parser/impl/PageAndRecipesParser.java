package com.mawai.mrcrawler.service.parser.impl;

import com.mawai.mrcrawler.model.Page;
import com.mawai.mrcrawler.model.PageAndRecipes;
import com.mawai.mrcrawler.model.Recipe;
import com.mawai.mrcrawler.service.parser.Parser;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PageAndRecipesParser implements Parser<PageAndRecipes> {

    @Autowired
    private PageParser pageParser;

    @Autowired
    private RecipeParser recipeParser;

    @Override
    public PageAndRecipes parse(Document document) {
        Page pagePared = pageParser.parse(document);
        List<Recipe> recipesParsed = recipeParser.parse(document);
        if (pagePared == null || recipesParsed == null) return null;
        return new PageAndRecipes(pagePared, recipesParsed);
    }
}
