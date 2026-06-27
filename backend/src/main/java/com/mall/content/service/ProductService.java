package com.mall.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.content.domain.entity.Product;
import com.mall.content.domain.entity.ProductImage;
import com.mall.content.domain.enums.ProductStatus;
import com.mall.content.mapper.ProductImageMapper;
import com.mall.content.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public Page<Product> list(int page, int size, String keyword, Long categoryId,
                              BigDecimal minPrice, BigDecimal maxPrice, String status, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean isAdmin = isAdmin(auth);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            wrapper.eq(Product::getUserId, userId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Product::getName, keyword).or().like(Product::getShortTitle, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (minPrice != null) {
            wrapper.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, maxPrice);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getUpdatedAt);
        return productMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Product requireAccessibleProduct(Long id, Authentication auth) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!isAdmin(auth) && !product.getUserId().equals((Long) auth.getPrincipal())) {
            throw new IllegalStateException("无权访问该商品");
        }
        return product;
    }

    /** 仅商品所有者可操作，管理员不可越权编辑/删除用户商品 */
    public Product requireProductOwner(Long id, Authentication auth) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!product.getUserId().equals((Long) auth.getPrincipal())) {
            throw new IllegalStateException("管理员只能查看商品，不可编辑或删除用户商品");
        }
        return product;
    }

    @Transactional
    public void deleteProduct(Long id, Authentication auth) {
        requireProductOwner(id, auth);
        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id)
        );
        for (ProductImage image : images) {
            storageService.deleteFile(image.getFilePath());
        }
        storageService.deleteProductDirectory(id);
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id));
        productMapper.deleteById(id);
    }

    @Transactional
    public void batchDelete(List<Long> ids, Authentication auth) {
        for (Long id : ids) {
            deleteProduct(id, auth);
        }
    }

    @Transactional
    public void batchUpdateStatus(List<Long> ids, String status, Authentication auth) {
        ProductStatus.valueOf(status);
        for (Long id : ids) {
            Product product = requireAccessibleProduct(id, auth);
            product.setStatus(status);
            productMapper.updateById(product);
        }
    }

    @Transactional
    public ProductImage uploadImage(Long productId, MultipartFile file, Authentication auth) throws Exception {
        requireAccessibleProduct(productId, auth);
        long count = productImageMapper.selectCount(
                new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId)
        );
        if (count >= 10) {
            throw new IllegalArgumentException("每个商品最多上传 10 张图片");
        }
        String path = storageService.saveProductImage(productId, file);
        ProductImage image = new ProductImage();
        image.setProductId(productId);
        image.setFilePath(path);
        image.setFileName(file.getOriginalFilename());
        image.setMimeType(file.getContentType());
        image.setFileSize(file.getSize());
        image.setSortOrder((int) count);
        image.setIsMain(count == 0 ? 1 : 0);
        productImageMapper.insert(image);
        syncMainImage(productId);
        return image;
    }

    @Transactional
    public void deleteImage(Long imageId, Authentication auth) {
        ProductImage image = productImageMapper.selectById(imageId);
        if (image == null) {
            throw new IllegalArgumentException("图片不存在");
        }
        requireAccessibleProduct(image.getProductId(), auth);
        storageService.deleteFile(image.getFilePath());
        productImageMapper.deleteById(imageId);
        reorderRemaining(image.getProductId());
        syncMainImage(image.getProductId());
    }

    @Transactional
    public void reorderImages(Long productId, List<Long> orderedIds, Authentication auth) {
        requireAccessibleProduct(productId, auth);
        for (int i = 0; i < orderedIds.size(); i++) {
            ProductImage image = productImageMapper.selectById(orderedIds.get(i));
            if (image == null || !image.getProductId().equals(productId)) {
                throw new IllegalArgumentException("图片排序数据无效");
            }
            image.setSortOrder(i);
            image.setIsMain(i == 0 ? 1 : 0);
            productImageMapper.updateById(image);
        }
        syncMainImage(productId);
    }

    public List<String> parseSellingPoints(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of(json);
        }
    }

    public String serializeSellingPoints(List<String> points) {
        try {
            return objectMapper.writeValueAsString(points);
        } catch (Exception ex) {
            throw new IllegalStateException("卖点序列化失败", ex);
        }
    }

    private void reorderRemaining(Long productId) {
        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, productId)
                        .orderByAsc(ProductImage::getSortOrder)
        );
        for (int i = 0; i < images.size(); i++) {
            ProductImage img = images.get(i);
            img.setSortOrder(i);
            img.setIsMain(i == 0 ? 1 : 0);
            productImageMapper.updateById(img);
        }
    }

    private void syncMainImage(Long productId) {
        ProductImage main = productImageMapper.selectOne(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, productId)
                        .orderByAsc(ProductImage::getSortOrder)
                        .last("LIMIT 1")
        );
        Product product = productMapper.selectById(productId);
        if (main != null) {
            product.setMainImageUrl(storageService.toPublicUrl(main.getFilePath()));
        } else {
            product.setMainImageUrl(null);
        }
        productMapper.updateById(product);
    }

    public boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ==================== 回收站（用户端） ====================

    public Page<Product> listUserDeletedProducts(Long userId, int page, int size) {
        Page<Product> pg = new Page<>(page, size);
        if (userId == null) {
            return productMapper.selectAllDeleted(pg);
        }
        return productMapper.selectDeletedByUserId(pg, userId);
    }

    @Transactional
    public void restoreUserProduct(Long userId, Long productId, boolean isAdmin) {
        Product product = productMapper.selectDeletedById(productId);
        if (product == null) throw new IllegalArgumentException("商品不存在或不在回收站中");
        if (!isAdmin && !product.getUserId().equals(userId)) throw new IllegalStateException("无权操作");
        productMapper.restoreDeleted(productId);
    }

    @Transactional
    public void purgeUserProduct(Long userId, Long productId, boolean isAdmin) {
        Product product = productMapper.selectDeletedById(productId);
        if (product == null) throw new IllegalArgumentException("商品不存在或不在回收站中");
        if (!isAdmin && !product.getUserId().equals(userId)) throw new IllegalStateException("无权操作");
        // 物理删除图片
        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        for (ProductImage img : images) {
            try { storageService.deleteFile(img.getFilePath()); } catch (Exception ignored) {}
            productImageMapper.deleteById(img.getId());
        }
        // 物理删除商品
        productMapper.deletePhysically(productId);
    }
}
