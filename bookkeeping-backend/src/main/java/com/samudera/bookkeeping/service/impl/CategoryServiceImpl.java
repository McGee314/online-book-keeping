package com.samudera.bookkeeping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samudera.bookkeeping.context.UserContext;
import com.samudera.bookkeeping.dto.CategoryRequest;
import com.samudera.bookkeeping.entity.Category;
import com.samudera.bookkeeping.entity.Transaction;
import com.samudera.bookkeeping.exception.BusinessException;
import com.samudera.bookkeeping.mapper.CategoryMapper;
import com.samudera.bookkeeping.mapper.TransactionMapper;
import com.samudera.bookkeeping.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final TransactionMapper transactionMapper;

    public CategoryServiceImpl(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    @Override
    public List<Category> listCurrentUserCategories() {
        Long userId = requireCurrentUserId();
        return lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(Category::getUserId, userId)
                        .or()
                        .isNull(Category::getUserId))
                .orderByAsc(Category::getType)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Category createCategory(CategoryRequest request) {
        Long userId = requireCurrentUserId();
        validateDuplicateName(userId, request.getName(), request.getType(), null);

        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName().trim());
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setDeleted(0);
        save(category);
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Category updateCategory(Long id, CategoryRequest request) {
        Long userId = requireCurrentUserId();
        Category category = getOwnedCategoryOrThrow(id);
        validateDuplicateName(userId, request.getName(), request.getType(), id);

        category.setName(request.getName().trim());
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        updateById(category);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Category category = getOwnedCategoryOrThrow(id);

        Long transactionCount = transactionMapper.selectCount(new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getUserId, category.getUserId())
                .eq(Transaction::getCategoryId, category.getId()));
        if (transactionCount != null && transactionCount > 0) {
            throw new BusinessException(409, "Cannot delete category with existing transactions");
        }

        removeById(id);
    }

    @Override
    public Category getOwnedCategoryOrThrow(Long id) {
        Long userId = requireCurrentUserId();
        Category category = getOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getId, id)
                .and(wrapper -> wrapper
                        .eq(Category::getUserId, userId)
                        .or()
                        .isNull(Category::getUserId))
                .last("LIMIT 1"));
        if (category == null) {
            throw new BusinessException(404, "Category not found");
        }
        return category;
    }

    private void validateDuplicateName(Long userId, String name, Integer type, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId)
                .eq(Category::getName, name.trim())
                .eq(Category::getType, type);
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }

        Long count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(409, "Category with the same name and type already exists");
        }
    }

    private Long requireCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "Unauthorized");
        }
        return userId;
    }
}