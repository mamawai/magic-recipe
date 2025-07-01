package com.mawai.mrmbplus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mawai.mrmbplus.model.MrCategory;
import com.mawai.mrmbplus.dao.MrCategoryMapper;
import com.mawai.mrmbplus.service.MrCategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 分类表 服务实现类
 * </p>
 *
 * @author mawai
 * @since 2025-06-30
 */
@Service
public class MrCategoryServiceImpl extends ServiceImpl<MrCategoryMapper, MrCategory> implements MrCategoryService {

    @Override
    public List<MrCategory> listCategories() {
        return baseMapper.selectList(new QueryWrapper<>());
    }
}
