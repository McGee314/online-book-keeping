package com.samudera.bookkeeping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samudera.bookkeeping.dto.CategoryRequest;
import com.samudera.bookkeeping.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> listCurrentUserCategories();

    Category createCategory(CategoryRequest request);

    Category updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);

    Category getOwnedCategoryOrThrow(Long id);
}