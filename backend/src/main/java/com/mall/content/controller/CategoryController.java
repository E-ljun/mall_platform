package com.mall.content.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.content.common.ApiResponse;
import com.mall.content.domain.entity.ProductCategory;
import com.mall.content.mapper.ProductCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ProductCategoryMapper categoryMapper;

    @GetMapping
    public ApiResponse<List<ProductCategory>> list() {
        List<ProductCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSortOrder)
        );
        return ApiResponse.ok(categories);
    }
}
