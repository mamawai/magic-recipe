package com.mawai.mrcrawler.service.db.impl;

import com.mawai.mrcrawler.model.*;
import com.mawai.mrcrawler.service.db.DbCacheService;
import com.mawai.mrmbplus.model.*;
import com.mawai.mrmbplus.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据库缓存服务实现类 - 使用现有实体类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbCacheServiceImpl implements DbCacheService {

    private final MrRecipeService mrRecipeService;
    private final MrCategoryService mrCategoryService;
    private final MrMaterialService mrMaterialService;
    private final MrRecipeStepService mrRecipeStepService;
    private final MrRecipeMaterialService mrRecipeMaterialService;
    private final MrRecipeCategoryService mrRecipeCategoryService;

    // 使用内存缓存存储分类映射关系，避免重复查询
    private final Map<String, Long> categoryCache = new ConcurrentHashMap<>();
    private final Map<String, Long> materialCache = new ConcurrentHashMap<>();

    @Override
    public Category getCategories() {
        try {
            // 从数据库查询所有分类
            List<MrCategory> categories = mrCategoryService.listCategories();

            if (categories != null && !categories.isEmpty()) {
                // 转换为爬虫模型
                Category category = new Category();
                List<CategoryGroup> groups = new ArrayList<>();
                
                // 按级别分组
                Map<Long, MrCategory> level1Map = new HashMap<>();
                Map<Long, List<MrCategory>> level2Children = new HashMap<>();
                Map<Long, List<MrCategory>> level3Children = new HashMap<>();
                
                // 先进行分类整理
                for (MrCategory cat : categories) {
                    // 更新分类缓存
                    categoryCache.put(cat.getName(), cat.getId());
                    
                    if (cat.getLevel() == 1) {
                        level1Map.put(cat.getId(), cat);
                        level2Children.put(cat.getId(), new ArrayList<>());
                    } else if (cat.getLevel() == 2) {
                        level3Children.put(cat.getId(), new ArrayList<>());
                        if (level2Children.containsKey(cat.getParentId())) {
                            level2Children.get(cat.getParentId()).add(cat);
                        }
                    } else if (cat.getLevel() == 3) {
                        if (level3Children.containsKey(cat.getParentId())) {
                            level3Children.get(cat.getParentId()).add(cat);
                        }
                    }
                }
                
                // 构建分类层次结构
                for (Map.Entry<Long, MrCategory> entry : level1Map.entrySet()) {
                    MrCategory level1Cat = entry.getValue();
                    CategoryGroup group = new CategoryGroup();
                    group.setName(level1Cat.getName());
                    List<CategorySubGroup> subGroups = new ArrayList<>();
                    
                    // 添加二级分类
                    List<MrCategory> level2Cats = level2Children.get(level1Cat.getId());
                    if (level2Cats != null) {
                        for (MrCategory level2Cat : level2Cats) {
                            CategorySubGroup subGroup = new CategorySubGroup();
                            subGroup.setName(level2Cat.getName());
                            subGroup.setLink(level2Cat.getCatId());
                            
                            // 添加三级分类
                            List<MrCategory> level3Cats = level3Children.get(level2Cat.getId());
                            if (level3Cats != null && !level3Cats.isEmpty()) {
                                List<CategoryType> types = new ArrayList<>();
                                for (MrCategory level3Cat : level3Cats) {
                                    CategoryType type = new CategoryType();
                                    type.setName(level3Cat.getName());
                                    type.setLink(level3Cat.getCatId());
                                    types.add(type);
                                }
                                subGroup.setTypes(types);
                            }
                            
                            subGroups.add(subGroup);
                        }
                    }
                    
                    group.setList(subGroups);
                    groups.add(group);
                }
                
                category.setCategories(groups);
                log.info("从数据库获取分类数据成功，共{}个一级分类", groups.size());
                return category;
            }
        } catch (Exception e) {
            log.error("从数据库获取分类数据失败: {}", e.getMessage(), e);
        }
        return null;
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCategories(Category category) {
        try {
            // 根据分类模型保存分类
            if (category != null && category.getCategories() != null) {
                // 处理一级分类
                for (CategoryGroup group : category.getCategories()) {
                    MrCategory parent = new MrCategory()
                            .setName(group.getName())
                            .setParentId(0L)
                            .setLevel(1)
                            .setCatId(null);

                    // 保存父分类
                    mrCategoryService.save(parent);
                    categoryCache.put(parent.getName(), parent.getId());

                    // 处理子分类
                    if (group.getList() != null) {
                        for (CategorySubGroup subGroup : group.getList()) {
                            MrCategory child = new MrCategory()
                                    .setName(subGroup.getName())
                                    .setParentId(parent.getId())
                                    .setLevel(2)
                                    .setCatId(subGroup.getLink() != null ? subGroup.getLink() : null);

                            mrCategoryService.save(child);
                            categoryCache.put(child.getName(), child.getId());

                            if (subGroup.getTypes() != null) {
                                for (CategoryType type : subGroup.getTypes()) {
                                    MrCategory typeCategory = new MrCategory()
                                            .setName(type.getName())
                                            .setParentId(child.getId())
                                            .setLevel(3)
                                            .setCatId(type.getLink() != null ? type.getLink() : null);

                                    mrCategoryService.save(typeCategory);
                                    categoryCache.put(typeCategory.getName(), typeCategory.getId());
                                }
                            }
                        }
                    }
                }
                log.info("保存分类数据到数据库");
            }
        } catch (Exception e) {
            log.error("保存分类数据到数据库失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public PageAndRecipes getCategoryRecipes(String category, int page) {
        try {
            Long categoryId = getCategoryId(category);
            if (categoryId != null) {
                // 根据分类ID查询相关的菜谱
                List<MrRecipeCategory> recipeCategories = mrRecipeCategoryService.lambdaQuery()
                        .eq(MrRecipeCategory::getCategoryId, categoryId)
                        .list();

                if (recipeCategories != null && !recipeCategories.isEmpty()) {
                    List<Long> recipeIds = recipeCategories.stream()
                            .map(MrRecipeCategory::getRecipeId)
                            .collect(Collectors.toList());

                    // 每页数量
                    int pageSize = 10;

                    // 计算总记录数
                    int total = recipeIds.size();

                    // 计算总页数
                    int totalPages = (int) Math.ceil((double) total / pageSize);

                    // 如果没有数据或者请求的页数超出范围，直接返回空结果
                    if (total == 0 || (page > totalPages && totalPages > 0)) {
                        log.info("数据库中分类数据不足，需要爬取更多数据: category={}, 请求页数={}, 当前总页数={}",
                                category, page, totalPages);
                        return null;
                    }

                    // 调整页码，确保在有效范围内
                    int effectivePage = Math.max(page, 1);

                    // 计算当前页的起始和结束索引
                    int start = (effectivePage - 1) * pageSize;
                    int end = Math.min(start + pageSize, total);

                    // 获取当前页的recipeIds
                    List<Long> pageRecipeIds = recipeIds.subList(start, end);

                    // 查询菜谱详情
                    List<MrRecipe> recipes = mrRecipeService.listByIds(pageRecipeIds);

                    // 构造返回结果
                    PageAndRecipes result = new PageAndRecipes();

                    if (recipes != null && !recipes.isEmpty()) {
                        // 将MrRecipe转换为爬虫模型中的Recipe
                        List<Recipe> recipeList = new ArrayList<>();

                        for (MrRecipe mrRecipe : recipes) {
                            Recipe recipe = new Recipe();
                            recipe.setName(mrRecipe.getTitle());
                            recipe.setUrl(mrRecipe.getUrlId()); // TODO: url看看需不需要拼前缀
                            recipe.setCover(mrRecipe.getCoverImg());
                            recipeList.add(recipe);
                        }

                        // 构造分页对象
                        Page pageObj = new Page();
                        pageObj.setCurrentPage(effectivePage);
                        if (effectivePage > 1) {
                            pageObj.setPrev("/category?name=" + category + "&page=" + (effectivePage-1));
                        }
                        if (effectivePage < totalPages) {
                            pageObj.setNext("/category?name=" + category + "&page=" + (effectivePage+1));
                        }

                        // 设置结果
                        result.setPage(pageObj);
                        result.setRecipes(recipeList);

                        log.info("从数据库获取分类食谱: category={}, 请求页数={}, 实际页数={}, 总页数={}, 结果数量={}",
                                category, page, effectivePage, totalPages, recipeList.size());

                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.error("从数据库获取分类食谱列表失败: {}", e.getMessage(), e);
        }
        return null;
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCategoryRecipes(String category, int page, PageAndRecipes pageAndRecipes) {
        try {
            if (pageAndRecipes != null && pageAndRecipes.getRecipes() != null) {
                // 获取或创建分类
                Long categoryId = getCategoryId(category);

                if (categoryId != null) {
                    // 保存每个菜谱
                    for (Recipe recipe : pageAndRecipes.getRecipes()) {
                        // 保存菜谱基本信息
                        MrRecipe mrRecipe = saveRecipe(recipe);

                        // 关联菜谱和分类
                        saveRecipeCategory(mrRecipe.getId(), categoryId);
                    }

                    log.info("保存分类食谱列表到数据库成功: 分类={}, 页码={}, 数量={}",
                            category, page, pageAndRecipes.getRecipes().size());
                }
            }
        } catch (Exception e) {
            log.error("保存分类食谱列表到数据库失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public PageAndRecipes getSearchRecipes(String keyword, int page) {
        try {
            // 每页数量
            int pageSize = 10;
            
            // 查询总记录数
            long total = mrRecipeService.lambdaQuery()
                    .like(MrRecipe::getTitle, keyword)
                    .count();
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);
            
            // 如果没有数据或者请求的页数超出范围，直接返回空结果
            if (total == 0 || (page > totalPages && totalPages > 0)) {
                log.info("数据库中数据不足，需要爬取更多数据: keyword={}, 请求页数={}, 当前总页数={}", 
                        keyword, page, totalPages);
                return null;
            }
            
            // 调整页码，确保在有效范围内
            int effectivePage = Math.max(page, 1);
            
            // 从数据库分页查询菜谱
            List<MrRecipe> recipes = mrRecipeService.lambdaQuery()
                    .like(MrRecipe::getTitle, keyword)
                    .orderByDesc(MrRecipe::getCreateTime)
                    .last("LIMIT " + (effectivePage - 1) * pageSize + ", " + pageSize)
                    .list();

            // 构造返回结果
            PageAndRecipes result = new PageAndRecipes();
            
            // 构造分页对象
            Page pageObj = new Page();
            pageObj.setCurrentPage(effectivePage);
            if (effectivePage > 1) {
                pageObj.setPrev("/search?keyword=" + keyword + "&page=" + (effectivePage-1));
            }
            if (effectivePage < totalPages) {
                pageObj.setNext("/search?keyword=" + keyword + "&page=" + (effectivePage+1));
            }
            
            // 将MrRecipe转换为爬虫模型中的Recipe
            List<Recipe> recipeList = new ArrayList<>();
            
            if (recipes != null && !recipes.isEmpty()) {
                for (MrRecipe mrRecipe : recipes) {
                    Recipe recipe = new Recipe();
                    recipe.setName(mrRecipe.getTitle());
                    recipe.setUrl(mrRecipe.getUrlId()); // TODO: url看看需不需要拼前缀
                    recipe.setCover(mrRecipe.getCoverImg());
                    recipeList.add(recipe);
                }
            }
            
            // 设置结果
            result.setPage(pageObj);
            result.setRecipes(recipeList);
            
            log.info("从数据库获取搜索结果: keyword={}, 请求页数={}, 实际页数={}, 总页数={}, 结果数量={}", 
                    keyword, page, effectivePage, totalPages, recipeList.size());
            
            return result;
        } catch (Exception e) {
            log.error("从数据库获取搜索结果失败: {}", e.getMessage(), e);
        }
        return null;
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSearchRecipes(String keyword, int page, PageAndRecipes pageAndRecipes) {
        try {
            // 搜索结果就是菜谱列表，直接保存菜谱信息
            if (pageAndRecipes != null && pageAndRecipes.getRecipes() != null) {
                for (Recipe recipe : pageAndRecipes.getRecipes()) {
                    saveRecipe(recipe);
                }
                log.info("保存搜索结果到数据库成功: 关键词={}, 页码={}, 数量={}",
                        keyword, page, pageAndRecipes.getRecipes().size());
            }
        } catch (Exception e) {
            log.error("保存搜索结果到数据库失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public RecipeContent getRecipeContent(String recipeNo) {
        try {
            // 根据URL标识查询菜谱
            MrRecipe recipe = mrRecipeService.lambdaQuery()
                    .eq(MrRecipe::getUrlId, recipeNo)
                    .one();

            if (recipe != null) {
                // 创建并填充RecipeContent对象
                RecipeContent recipeContent = new RecipeContent();
                recipeContent.setName(recipe.getTitle());
                recipeContent.setCover(recipe.getCoverImg());
                
                // 查询菜谱步骤
                List<MrRecipeStep> steps = mrRecipeStepService.lambdaQuery()
                        .eq(MrRecipeStep::getRecipeId, recipe.getId())
                        .orderByAsc(MrRecipeStep::getStepNumber)
                        .list();

                if (steps != null && !steps.isEmpty()) {
                    // 转换步骤格式
                    List<Step> stepList = new ArrayList<>();
                    for (MrRecipeStep step : steps) {
                        Step s = new Step();
                        s.setImg(step.getImageUrl());
                        s.setDesc(step.getDescription());
                        stepList.add(s);
                    }
                    recipeContent.setSteps(stepList);
                }

                // 查询菜谱食材
                List<MrRecipeMaterial> recipeMaterials = mrRecipeMaterialService.lambdaQuery()
                        .eq(MrRecipeMaterial::getRecipeId, recipe.getId())
                        .orderByAsc(MrRecipeMaterial::getOrderNum)
                        .list();

                if (recipeMaterials != null && !recipeMaterials.isEmpty()) {
                    // 获取食材ID列表
                    List<Long> materialIds = recipeMaterials.stream()
                            .map(MrRecipeMaterial::getMaterialId)
                            .collect(Collectors.toList());

                    // 查询食材详情
                    List<MrMaterial> materials = mrMaterialService.listByIds(materialIds);
                    Map<Long, MrMaterial> materialMap = materials.stream()
                            .collect(Collectors.toMap(MrMaterial::getId, m -> m, (v1, v2) -> v1));

                    // 组装食材列表
                    List<Material> materialList = new ArrayList<>();
                    for (MrRecipeMaterial rm : recipeMaterials) {
                        MrMaterial mrMaterial = materialMap.get(rm.getMaterialId());
                        if (mrMaterial != null) {
                            Material m = new Material();
                            m.setName(mrMaterial.getName());
                            m.setUnit(rm.getAmount());
                            materialList.add(m);
                        }
                    }
                    recipeContent.setMaterials(materialList);
                }

                return recipeContent;
            }
        } catch (Exception e) {
            log.error("从数据库获取食谱详情失败: {}", e.getMessage(), e);
        }
        return null;
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRecipeContent(String recipeNo, RecipeContent recipeContent) {
        try {
            if (recipeContent != null) {
                MrRecipe mrRecipe = mrRecipeService.lambdaQuery()
                        .eq(MrRecipe::getUrlId, recipeNo)
                        .one(); // 先查询菜谱，如果存在则更新，不存在则创建
                if (mrRecipe == null) mrRecipe = new MrRecipe();
                mrRecipe.setTitle(recipeContent.getName());
                mrRecipe.setUrlId(recipeNo);
                mrRecipe.setCoverImg(recipeContent.getCover());
                mrRecipe.setCreateTime(LocalDateTime.now());
                mrRecipe.setUpdateTime(LocalDateTime.now());
                mrRecipeService.save(mrRecipe);

                // 保存步骤
                if (recipeContent.getSteps() != null) {
                    List<MrRecipeStep> mrSteps = getSteps(recipeContent, mrRecipe);
                    mrRecipeStepService.saveBatch(mrSteps);
                }

                // 保存食材
                if (recipeContent.getMaterials() != null) {
                    int orderNum = 1;
                    List<MrRecipeMaterial> recipeMaterials = new ArrayList<>();
                    
                    for (Material material : recipeContent.getMaterials()) {
                        // 直接创建新食材
                        MrMaterial mrMaterial = new MrMaterial();
                        mrMaterial.setName(material.getName());
                        mrMaterialService.save(mrMaterial);
                        
                        // 创建菜谱食材关联
                        MrRecipeMaterial mrRecipeMaterial = new MrRecipeMaterial();
                        mrRecipeMaterial.setRecipeId(mrRecipe.getId());
                        mrRecipeMaterial.setMaterialId(mrMaterial.getId());
                        mrRecipeMaterial.setAmount(material.getUnit());
                        mrRecipeMaterial.setOrderNum(orderNum++);
                        recipeMaterials.add(mrRecipeMaterial);
                    }
                    
                    if (!recipeMaterials.isEmpty()) {
                        mrRecipeMaterialService.saveBatch(recipeMaterials);
                    }
                }

                log.info("保存食谱详情到数据库成功: recipeNo={}, title={}", recipeNo, recipeContent.getName());
            }
        } catch (Exception e) {
            log.error("保存食谱详情到数据库失败: {}", e.getMessage(), e);
        }
    }

    @NotNull
    private static List<MrRecipeStep> getSteps(RecipeContent recipeContent, MrRecipe mrRecipe) {
        List<Step> steps = recipeContent.getSteps();
        List<MrRecipeStep> mrSteps = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            MrRecipeStep mrStep = new MrRecipeStep();
            mrStep.setRecipeId(mrRecipe.getId());
            mrStep.setStepNumber(i + 1);
            mrStep.setDescription(step.getDesc());
            mrStep.setImageUrl(step.getImg());
            mrSteps.add(mrStep);
        }
        return mrSteps;
    }


    /**
     * 从URL中提取菜谱urlId
     */
    public String extractRecipeId(String url) {
        int start = url.indexOf("/recipe/") + 8;
        int end = url.indexOf("/", start);
        if (end == -1) {
            end = url.length();
        }
        return url.substring(start, end);
    }


    /**
     * 保存菜谱基本信息
     */
    private MrRecipe saveRecipe(Recipe recipe) {
        // 先查询是否存在
        String urlId = extractRecipeId(recipe.getUrl());
        MrRecipe existingRecipe = mrRecipeService.lambdaQuery()
                .eq(MrRecipe::getUrlId, urlId)
                .one();

        if (existingRecipe != null) {
            return existingRecipe;
        }

        // 不存在则创建
        MrRecipe mrRecipe = new MrRecipe();
        mrRecipe.setTitle(recipe.getName());
        mrRecipe.setUrlId(urlId);
        mrRecipe.setCoverImg(recipe.getCover());
        mrRecipe.setCreateTime(LocalDateTime.now());
        mrRecipe.setUpdateTime(LocalDateTime.now());

        mrRecipeService.save(mrRecipe);
        return mrRecipe;
    }

    /**
     * 保存菜谱与分类的关联关系
     */
    private void saveRecipeCategory(Long recipeId, Long categoryId) {
        // 检查是否已存在关联
        boolean exists = mrRecipeCategoryService.lambdaQuery()
                .eq(MrRecipeCategory::getRecipeId, recipeId)
                .eq(MrRecipeCategory::getCategoryId, categoryId)
                .exists();

        if (!exists) {
            MrRecipeCategory recipeCategory = new MrRecipeCategory();
            recipeCategory.setRecipeId(recipeId);
            recipeCategory.setCategoryId(categoryId);

            mrRecipeCategoryService.save(recipeCategory);
        }
    }

    /**
     * 根据分类名称获取分类ID
     *
     * @param categoryName catId
     */
    private Long getCategoryId(String categoryName) {
        // 先从缓存中获取
        Long categoryId = categoryCache.get(categoryName);

        if (categoryId == null) {
            // 缓存中不存在，从数据库查询
            MrCategory category = mrCategoryService.lambdaQuery()
                    .eq(MrCategory::getCatId, categoryName)
                    .one();

            if (category != null) {
                categoryId = category.getId();
                // 更新缓存
                categoryCache.put(categoryName, categoryId);
            }
        }

        return categoryId;
    }
}