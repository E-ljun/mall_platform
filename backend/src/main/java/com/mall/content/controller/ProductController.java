package com.mall.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.content.common.ApiResponse;
import com.mall.content.domain.entity.Product;
import com.mall.content.domain.entity.ProductImage;
import com.mall.content.domain.enums.ProductStatus;
import com.mall.content.mapper.ProductImageMapper;
import com.mall.content.mapper.ProductMapper;
import com.mall.content.service.ProductService;
import com.mall.content.service.StorageService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductService productService;
    private final StorageService storageService;
    private final com.mall.content.mapper.SysUserMapper sysUserMapper;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String status,
            Authentication auth
    ) {
        Page<Product> result = productService.list(page, size, keyword, categoryId, minPrice, maxPrice, status, auth);
        // 为每条记录附加创建者用户名
        List<Map<String, Object>> records = result.getRecords().stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("price", p.getPrice());
            m.put("stock", p.getStock());
            m.put("categoryId", p.getCategoryId());
            m.put("status", p.getStatus());
            m.put("mainImageUrl", p.getMainImageUrl());
            m.put("userId", p.getUserId());
            m.put("shortTitle", p.getShortTitle());
            m.put("detailContent", p.getDetailContent());
            m.put("createdAt", p.getCreatedAt());
            m.put("updatedAt", p.getUpdatedAt());
            var creator = sysUserMapper.selectById(p.getUserId());
            m.put("ownerUsername", creator != null ? creator.getUsername() : "未知");
            return m;
        }).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<Product> create(@RequestBody ProductCreateRequest request, Authentication auth) {
        Product product = new Product();
        product.setUserId((Long) auth.getPrincipal());
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCategoryId(request.getCategoryId());
        product.setStock(request.getStock() != null ? request.getStock() : 0);
        product.setStatus(ProductStatus.DRAFT.name());
        productMapper.insert(product);
        return ApiResponse.ok(product);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id, Authentication auth) {
        Product product = productService.requireAccessibleProduct(id, auth);
        List<ProductImage> images = productImageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSortOrder)
        );
        List<Map<String, Object>> imageViews = images.stream().map(img -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", img.getId());
            m.put("fileName", img.getFileName());
            m.put("sortOrder", img.getSortOrder());
            m.put("isMain", img.getIsMain());
            m.put("url", storageService.toPublicUrl(img.getFilePath()));
            return m;
        }).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("product", product);
        data.put("images", imageViews);
        data.put("sellingPoints", productService.parseSellingPoints(product.getSellingPoints()));
        return ApiResponse.ok(data);
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @RequestBody ProductUpdateRequest request, Authentication auth) {
        Product product = productService.requireProductOwner(id, auth);
        if (request.getName() != null) product.setName(request.getName());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getCategoryId() != null) product.setCategoryId(request.getCategoryId());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        if (request.getShortTitle() != null) product.setShortTitle(request.getShortTitle());
        if (request.getDetailContent() != null) product.setDetailContent(request.getDetailContent());
        if (request.getSellingPoints() != null) {
            product.setSellingPoints(productService.serializeSellingPoints(request.getSellingPoints()));
        }
        productMapper.updateById(product);
        return ApiResponse.ok(product);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) {
        productService.deleteProduct(id, auth);
        return ApiResponse.ok(null);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody BatchIdsRequest request, Authentication auth) {
        productService.batchDelete(request.getIds(), auth);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/batch-status")
    public ApiResponse<Void> batchStatus(@RequestBody BatchStatusRequest request, Authentication auth) {
        productService.batchUpdateStatus(request.getIds(), request.getStatus(), auth);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/images")
    public ApiResponse<Map<String, Object>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) throws Exception {
        ProductImage image = productService.uploadImage(id, file, auth);
        Map<String, Object> view = new HashMap<>();
        view.put("id", image.getId());
        view.put("fileName", image.getFileName());
        view.put("url", storageService.toPublicUrl(image.getFilePath()));
        view.put("sortOrder", image.getSortOrder());
        view.put("isMain", image.getIsMain());
        return ApiResponse.ok(view);
    }

    @PostMapping("/{id}/images/batch")
    public ApiResponse<List<Map<String, Object>>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            Authentication auth
    ) throws Exception {
        if (files.length > 10) {
            return ApiResponse.fail("TOO_MANY", "单次最多上传 10 张图片");
        }
        List<Map<String, Object>> views = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            ProductImage image = productService.uploadImage(id, file, auth);
            Map<String, Object> view = new HashMap<>();
            view.put("id", image.getId());
            view.put("fileName", image.getFileName());
            view.put("url", storageService.toPublicUrl(image.getFilePath()));
            views.add(view);
        }
        return ApiResponse.ok(views);
    }

    @DeleteMapping("/images/{imageId}")
    public ApiResponse<Void> deleteImage(@PathVariable Long imageId, Authentication auth) {
        productService.deleteImage(imageId, auth);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/images/reorder")
    public ApiResponse<Void> reorderImages(
            @PathVariable Long id,
            @RequestBody BatchIdsRequest request,
            Authentication auth
    ) {
        productService.reorderImages(id, request.getIds(), auth);
        return ApiResponse.ok(null);
    }

    @Data
    public static class ProductCreateRequest {
        @NotBlank
        private String name;
        @NotNull
        private BigDecimal price;
        private Long categoryId;
        private Integer stock;
    }

    @Data
    public static class ProductUpdateRequest {
        private String name;
        private BigDecimal price;
        private Long categoryId;
        private Integer stock;
        private String status;
        private String shortTitle;
        private String detailContent;
        private List<String> sellingPoints;
    }

    @Data
    public static class BatchIdsRequest {
        @NotEmpty
        private List<Long> ids;
    }

    // ==================== 图片导出 ====================

    @GetMapping("/images/{imageId}/export")
    public org.springframework.http.ResponseEntity<byte[]> exportImage(@PathVariable Long imageId, Authentication auth) {
        ProductImage image = productImageMapper.selectById(imageId);
        if (image == null) {
            throw new IllegalArgumentException("图片不存在");
        }
        productService.requireAccessibleProduct(image.getProductId(), auth);
        byte[] bytes = storageService.readFile(image.getFilePath());
        String mime = storageService.resolveMimeType(image.getFilePath());
        String ext = image.getFileName() != null && image.getFileName().contains(".")
                ? image.getFileName().substring(image.getFileName().lastIndexOf("."))
                : ".jpg";
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"image-" + imageId + ext + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(mime))
                .body(bytes);
    }

    @PostMapping("/{id}/images/export-batch")
    public org.springframework.http.ResponseEntity<byte[]> exportImagesBatch(
            @PathVariable Long id,
            @RequestBody BatchIdsRequest request,
            Authentication auth
    ) throws Exception {
        productService.requireAccessibleProduct(id, auth);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            for (Long imageId : request.getIds()) {
                ProductImage image = productImageMapper.selectById(imageId);
                if (image == null || !image.getProductId().equals(id)) continue;
                byte[] bytes = storageService.readFile(image.getFilePath());
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(image.getFileName() != null ? image.getFileName() : "image-" + imageId + ".jpg");
                zos.putNextEntry(entry);
                zos.write(bytes);
                zos.closeEntry();
            }
        }
        String filename = java.net.URLEncoder.encode("product-" + id + "-images.zip", java.nio.charset.StandardCharsets.UTF_8);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename)
                .contentType(org.springframework.http.MediaType.parseMediaType("application/zip"))
                .body(baos.toByteArray());
    }

    @Data
    public static class BatchStatusRequest {
        @NotEmpty
        private List<Long> ids;
        @NotBlank
        private String status;
    }
}
