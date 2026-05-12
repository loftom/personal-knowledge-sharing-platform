package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.Category;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.ContentTag;
import com.knowledge.platform.domain.entity.Tag;
import com.knowledge.platform.domain.mapper.CategoryMapper;
import com.knowledge.platform.domain.mapper.ContentMapper;
import com.knowledge.platform.domain.mapper.ContentTagMapper;
import com.knowledge.platform.domain.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaxonomyService {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ContentMapper contentMapper;
    private final ContentTagMapper contentTagMapper;

    public TaxonomyService(CategoryMapper categoryMapper, TagMapper tagMapper,
                          ContentMapper contentMapper, ContentTagMapper contentTagMapper) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.contentMapper = contentMapper;
        this.contentTagMapper = contentTagMapper;
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

    public void updateCategory(Long id, Phase2Dtos.CreateCategoryRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new AppException("分类不存在");
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            Category existing = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                    .eq(Category::getName, request.getName())
                    .ne(Category::getId, id));
            if (existing != null) {
                throw new AppException("分类名称已存在");
            }
            category.setName(request.getName());
        }
        if (request.getParentId() != null) {
            category.setParentId(request.getParentId());
        }
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }
        if (request.getEnabled() != null) {
            category.setEnabled(request.getEnabled());
        }
        categoryMapper.updateById(category);
    }

    public void deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new AppException("分类不存在");
        }
        long useCount = contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getCategoryId, id)
                .eq(Content::getStatus, "PUBLISHED"));
        if (useCount > 0) {
            throw new AppException("该分类下有已发布内容，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    public void updateTag(Long id, Phase2Dtos.CreateTagRequest request) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new AppException("标签不存在");
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            Tag existing = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getName, request.getName())
                    .ne(Tag::getId, id));
            if (existing != null) {
                throw new AppException("标签名称已存在");
            }
            tag.setName(request.getName());
        }
        tagMapper.updateById(tag);
    }

    public void deleteTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new AppException("标签不存在");
        }
        long useCount = contentTagMapper.selectCount(new LambdaQueryWrapper<ContentTag>()
                .eq(ContentTag::getTagId, id));
        if (useCount > 0) {
            throw new AppException("该标签已被内容使用，无法删除");
        }
        tagMapper.deleteById(id);
    }
}
