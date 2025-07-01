package com.mawai.mrmbplus.service;

import com.mawai.mrmbplus.model.MrCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 分类表 服务类
 * </p>
 *
 * @author mawai
 * @since 2025-06-30
 */
public interface MrCategoryService extends IService<MrCategory> {

    List<MrCategory> listCategories();
}
