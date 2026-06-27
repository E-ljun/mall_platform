package com.mall.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.content.common.ApiResponse;
import com.mall.content.domain.entity.Product;
import com.mall.content.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/recycle-bin")
@RequiredArgsConstructor
public class RecycleBinController {

    private final ProductService productService;

    @GetMapping("/products")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        Long userId = (Long) auth.getPrincipal();
        boolean isAdmin = productService.isAdmin(auth);

        Page<Product> result;
        if (isAdmin) {
            // 管理员看全部已删除商品
            result = productService.listUserDeletedProducts(null, page, size);
        } else {
            result = productService.listUserDeletedProducts(userId, page, size);
        }

        return ApiResponse.ok(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @PostMapping("/products/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean isAdmin = productService.isAdmin(auth);
        productService.restoreUserProduct(userId, id, isAdmin);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> purge(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean isAdmin = productService.isAdmin(auth);
        productService.purgeUserProduct(userId, id, isAdmin);
        return ApiResponse.ok(null);
    }
}
