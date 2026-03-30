package com.zhihu.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihu.platform.common.AppException;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.domain.entity.Category;
import com.zhihu.platform.domain.entity.Tag;
import com.zhihu.platform.domain.mapper.CategoryMapper;
import com.zhihu.platform.domain.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaxonomyService {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public TaxonomyService(CategoryMapper categoryMapper, TagMapper tagMapper) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    public List<Phase2Dtos.CategoryOption> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getEnabled, 1)
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId))
                .stream()
                .map(category -> {
                    Phase2Dtos.CategoryOption item = new Phase2Dtos.CategoryOption();
                    item.setId(category.getId());
                    item.setParentId(category.getParentId());
                    item.setName(category.getName());
                    return item;
                })
                .toList();
    }

    public List<Phase2Dtos.TagOption> tags() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                        .orderByAsc(Tag::getName))
                .stream()
                .map(tag -> {
                    Phase2Dtos.TagOption item = new Phase2Dtos.TagOption();
                    item.setId(tag.getId());
                    item.setName(tag.getName());
                    return item;
                })
                .toList();
    }

    public Long createCategory(Phase2Dtos.CreateCategoryRequest request) {
        Category existing = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, request.getName()));
        if (existing != null) {
            throw new AppException("Category already exists");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        categoryMapper.insert(category);
        return category.getId();
    }

    public Long createTag(Phase2Dtos.CreateTagRequest request) {
        Tag existing = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, request.getName()));
        if (existing != null) {
            throw new AppException("Tag already exists");
        }
        Tag tag = new Tag();
        tag.setName(request.getName());
        tagMapper.insert(tag);
        return tag.getId();
    }
}
