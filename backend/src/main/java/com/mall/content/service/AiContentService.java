package com.mall.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.content.ai.AiChatClient;
import com.mall.content.ai.dto.MarketingCopyResult;
import com.mall.content.ai.dto.ProductDescriptionResult;
import com.mall.content.ai.prompt.DetailImagePrompts;
import com.mall.content.ai.prompt.MarketingCopyPrompts;
import com.mall.content.ai.prompt.ProductDescriptionPrompts;
import com.mall.content.config.MallProperties;
import com.mall.content.domain.entity.AiGenerationLog;
import com.mall.content.domain.entity.MarketingCopy;
import com.mall.content.domain.entity.Product;
import com.mall.content.domain.entity.ProductImage;
import com.mall.content.domain.entity.SysUser;
import com.mall.content.domain.enums.MarketingPlatform;
import com.mall.content.mapper.AiGenerationLogMapper;
import com.mall.content.mapper.MarketingCopyMapper;
import com.mall.content.mapper.SysUserMapper;
import com.mall.content.mapper.ProductImageMapper;
import com.mall.content.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiContentService {

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final MarketingCopyMapper marketingCopyMapper;
    private final AiGenerationLogMapper aiGenerationLogMapper;
    private final SysUserMapper sysUserMapper;
    private final AiChatClient aiClient;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final MallProperties mallProperties;
    private final ProductService productService;
    private final AdminService adminService;

    @Transactional
    public ProductDescriptionResult generateProductDescription(
            Long productId, Authentication auth, List<Long> imageIds, String keywords
    ) {
        checkAiQuota(auth, "description");
        long start = System.currentTimeMillis();
        Long userId = (Long) auth.getPrincipal();
        Product product = productService.requireAccessibleProduct(productId, auth);
        List<ProductImage> images = loadSelectedImages(productId, imageIds);
        List<String> summaries = images.stream()
                .map(img -> "file=" + img.getFileName() + "; sort=" + img.getSortOrder())
                .toList();
        List<String> base64Images = images.stream()
                .map(img -> "data:" + img.getMimeType() + ";base64,"
                        + Base64.getEncoder().encodeToString(storageService.readFile(img.getFilePath())))
                .toList();

        String providerName = mallProperties.getAi().getRouting().getProductDescription();
        var provider = mallProperties.getAi().requireProvider(providerName);
        String model = provider.getVisionModel() != null ? provider.getVisionModel() : provider.getTextModel();

        String prompt = ProductDescriptionPrompts.buildProductDescriptionPrompt(
                product.getName(), keywords, summaries, base64Images
        );

        try {
            String json = aiClient.generateStructuredJson(
                    providerName, model, "Return strict JSON only.", prompt, base64Images
            );
            ProductDescriptionResult result = parseOrRepair(json, providerName, provider.getTextModel());
            persistDescription(product, result, keywords, json);
            logAi(userId, productId, "PRODUCT_DESC", null, model, "SUCCESS", keywords, null, start);
            adminService.consumeQuota(userId);
            return result;
        } catch (Exception ex) {
            logAi(userId, productId, "PRODUCT_DESC", null, model, "FAILED", keywords, ex.getMessage(), start);
            throw new IllegalStateException("AI 生成失败：" + ex.getMessage() + "，请检查 API Key 或稍后重试。", ex);
        }
    }

    @Transactional
    public MarketingCopyResult generateMarketingCopy(
            Long productId, Authentication auth, MarketingPlatform platform, int variantCount
    ) {
        checkAiQuota(auth, "copy");
        long start = System.currentTimeMillis();
        Long userId = (Long) auth.getPrincipal();
        Product product = productService.requireAccessibleProduct(productId, auth);
        if (product.getShortTitle() == null || product.getDetailContent() == null) {
            throw new IllegalStateException("请先生成商品描述，再生成营销文案。");
        }

        String providerName = mallProperties.getAi().getRouting().getMarketingCopy();
        var provider = mallProperties.getAi().requireProvider(providerName);
        String model = provider.getTextModel();

        String prompt = MarketingCopyPrompts.buildMarketingCopyPrompt(
                platform, product.getName(), product.getShortTitle(),
                product.getSellingPoints(), product.getDetailContent(), variantCount
        );

        try {
            String json = aiClient.generateStructuredJson(
                    providerName, model, "Return strict JSON only.", prompt, List.of()
            );
            MarketingCopyResult result = aiClient.parseJson(json, MarketingCopyResult.class);
            persistMarketingCopies(productId, userId, platform, result);
            trimCopies(productId, 3); // 每个商品最多保留 3 条文案
            logAi(userId, productId, "MARKETING_COPY", platform.name(), model, "SUCCESS", platform.label(), null, start);
            adminService.consumeQuota(userId);
            return result;
        } catch (Exception ex) {
            logAi(userId, productId, "MARKETING_COPY", platform.name(), model, "FAILED", platform.label(), ex.getMessage(), start);
            throw new IllegalStateException("营销文案生成失败：" + ex.getMessage(), ex);
        }
    }

    /**
     * 调用 Wan 2.7 生成商品详情页模块图，结果自动存入商品图片库。
     */
    @Transactional
    public ProductImage generateDetailImage(
            Long productId, Authentication auth,
            String sectionTitle, String sectionCopy,
            String visualDirection, String aspectRatio
    ) {
        checkAiQuota(auth, "image");
        long start = System.currentTimeMillis();
        Long userId = (Long) auth.getPrincipal();
        Product product = productService.requireAccessibleProduct(productId, auth);

        String providerName = mallProperties.getAi().getDefaultProvider();
        var provider = mallProperties.getAi().requireProvider(providerName);
        String model = provider.getImageModel();
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("当前 AI 服务商未配置图像生成模型。");
        }

        // 加载商品参考图，传给 Wan 2.7 以确保生成图片与商品相关
        List<ProductImage> productImages = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, productId)
                        .orderByAsc(ProductImage::getSortOrder)
        );
        List<String> refImages = new ArrayList<>();
        for (ProductImage img : productImages) {
            try {
                byte[] bytes = storageService.readFile(img.getFilePath());
                String b64 = "data:" + img.getMimeType() + ";base64,"
                        + Base64.getEncoder().encodeToString(bytes);
                refImages.add(b64);
            } catch (Exception ignored) { /* 图片不可用则跳过 */ }
        }

        String prompt = DetailImagePrompts.buildSectionImagePrompt(
                product.getName(),
                sectionTitle != null ? sectionTitle : "商品详情",
                sectionCopy != null ? sectionCopy : product.getDetailContent(),
                visualDirection != null ? visualDirection : "简洁高端电商风格，白底或浅色渐变背景，突出商品主体",
                aspectRatio != null ? aspectRatio : "3:4",
                !refImages.isEmpty()
        );

        try {
            String rawResult = aiClient.generateImage(providerName, model, prompt, "1024x1024", 1, refImages);
            if (rawResult == null || rawResult.isBlank()) {
                throw new IllegalStateException("图像生成返回空数据，请稍后重试。");
            }

            byte[] imageBytes;
            if (rawResult.startsWith("http://") || rawResult.startsWith("https://")) {
                // Wan 2.7 直接返回 OSS URL，下载图片字节
                imageBytes = new java.net.URL(rawResult).openStream().readAllBytes();
            } else if (rawResult.contains(",")) {
                // data:image/png;base64,... 格式
                String pureBase64 = rawResult.substring(rawResult.indexOf(",") + 1);
                imageBytes = Base64.getDecoder().decode(pureBase64);
            } else {
                // 纯 base64
                imageBytes = Base64.getDecoder().decode(rawResult);
            }

            // 存到商品图片目录
            String storedPath = storageService.saveGeneratedImage(productId, imageBytes, "ai_gen_" + System.currentTimeMillis() + ".png");

            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setFilePath(storedPath);
            image.setFileName("AI生成_" + (sectionTitle != null ? sectionTitle : "详情图") + ".png");
            image.setMimeType("image/png");
            image.setFileSize((long) imageBytes.length);
            image.setSortOrder(999); // 末尾
            image.setIsMain(0);
            productImageMapper.insert(image);

            // 修正 sortOrder 为实际插入顺序
            List<ProductImage> all = productImageMapper.selectList(
                    new LambdaQueryWrapper<ProductImage>()
                            .eq(ProductImage::getProductId, productId)
                            .orderByAsc(ProductImage::getSortOrder)
            );
            if (!all.isEmpty()) {
                image.setSortOrder(all.get(all.size() - 1).getSortOrder() + 1);
                productImageMapper.updateById(image);
            }

            logAi(userId, productId, "DETAIL_IMAGE", null, model, "SUCCESS",
                    "section=" + sectionTitle, null, start);
            adminService.consumeQuota(userId);
            return image;
        } catch (Exception ex) {
            logAi(userId, productId, "DETAIL_IMAGE", null,
                    provider.getImageModel(), "FAILED",
                    "section=" + sectionTitle, ex.getMessage(), start);
            throw new IllegalStateException("详情图生成失败：" + ex.getMessage(), ex);
        }
    }

    public List<MarketingCopy> listCopies(Long productId, Authentication auth) {
        productService.requireAccessibleProduct(productId, auth);
        return marketingCopyMapper.selectList(
                new LambdaQueryWrapper<MarketingCopy>()
                        .eq(MarketingCopy::getProductId, productId)
                        .orderByDesc(MarketingCopy::getUpdatedAt)
        );
    }

    @Transactional
    public MarketingCopy updateCopy(Long copyId, String title, String content, Authentication auth) {
        MarketingCopy copy = marketingCopyMapper.selectById(copyId);
        if (copy == null) {
            throw new IllegalArgumentException("文案不存在");
        }
        productService.requireAccessibleProduct(copy.getProductId(), auth);
        copy.setTitle(title);
        copy.setContent(content);
        copy.setIsDraft(0);
        copy.setSource("MANUAL");
        marketingCopyMapper.updateById(copy);
        return copy;
    }

    @Transactional
    public MarketingCopy toggleFavorite(Long copyId, Authentication auth) {
        MarketingCopy copy = marketingCopyMapper.selectById(copyId);
        if (copy == null) {
            throw new IllegalArgumentException("文案不存在");
        }
        productService.requireAccessibleProduct(copy.getProductId(), auth);
        copy.setIsFavorite(copy.getIsFavorite() != null && copy.getIsFavorite() == 1 ? 0 : 1);
        marketingCopyMapper.updateById(copy);
        return copy;
    }

    @Transactional
    public void deleteCopy(Long copyId, Authentication auth) {
        MarketingCopy copy = marketingCopyMapper.selectById(copyId);
        if (copy == null) {
            throw new IllegalArgumentException("文案不存在");
        }
        productService.requireAccessibleProduct(copy.getProductId(), auth);
        marketingCopyMapper.deleteById(copyId);
    }

    /**
     * 清理超出限额的旧文案，保留最新的 maxCount 条。
     */
    @Transactional
    public void trimCopies(Long productId, int maxCount) {
        List<MarketingCopy> all = marketingCopyMapper.selectList(
                new LambdaQueryWrapper<MarketingCopy>()
                        .eq(MarketingCopy::getProductId, productId)
                        .orderByDesc(MarketingCopy::getCreatedAt)
        );
        if (all.size() > maxCount) {
            for (int i = maxCount; i < all.size(); i++) {
                marketingCopyMapper.deleteById(all.get(i).getId());
            }
        }
    }

    private ProductDescriptionResult parseOrRepair(String json, String providerName, String textModel) {
        try {
            return aiClient.parseJson(json, ProductDescriptionResult.class);
        } catch (Exception ex) {
            String fallbackProvider = mallProperties.getAi().getRouting().getFallbackText();
            var fallback = mallProperties.getAi().requireProvider(fallbackProvider);
            String repairedJson = aiClient.generateStructuredJson(
                    fallbackProvider,
                    fallback.getTextModel() != null ? fallback.getTextModel() : textModel,
                    "Return strict JSON only.",
                    ProductDescriptionPrompts.buildRepairPrompt(json),
                    List.of()
            );
            return aiClient.parseJson(repairedJson, ProductDescriptionResult.class);
        }
    }

    private void persistDescription(Product product, ProductDescriptionResult result, String keywords, String rawJson)
            throws JsonProcessingException {
        product.setShortTitle(result.getShortTitle());
        product.setSellingPoints(objectMapper.writeValueAsString(result.getSellingPoints()));
        product.setDetailContent(result.getDetailContent());
        product.setKeywords(keywords);
        product.setAiAnalysisRaw(rawJson);
        productMapper.updateById(product);
    }

    private void persistMarketingCopies(Long productId, Long userId, MarketingPlatform platform, MarketingCopyResult result)
            throws JsonProcessingException {
        for (MarketingCopyResult.Variant variant : result.getVariants()) {
            MarketingCopy copy = new MarketingCopy();
            copy.setProductId(productId);
            copy.setUserId(userId);
            copy.setPlatform(platform.name());
            copy.setVariantNo(variant.getVariantNo());
            copy.setTitle(variant.getTitle());
            copy.setContent(variant.getContent());
            copy.setHashtags(objectMapper.writeValueAsString(variant.getHashtags()));
            copy.setIsDraft(1);
            copy.setIsFavorite(0);
            copy.setSource("AI");
            marketingCopyMapper.insert(copy);
        }
    }

    private List<ProductImage> loadSelectedImages(Long productId, List<Long> imageIds) {
        LambdaQueryWrapper<ProductImage> wrapper = new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId)
                .orderByAsc(ProductImage::getSortOrder);
        if (imageIds != null && !imageIds.isEmpty()) {
            wrapper.in(ProductImage::getId, imageIds);
        }
        List<ProductImage> images = productImageMapper.selectList(wrapper);
        if (images.isEmpty()) {
            throw new IllegalStateException("请至少选择一张商品图片");
        }
        return images;
    }

    private void checkAiQuota(Authentication auth, String module) {
        Long userId = (Long) auth.getPrincipal();
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null && !adminService.canUseModule(user, module)) {
            if (user.getQuotaTotal() != null && user.getQuotaTotal() != -1
                    && user.getQuotaUsed() != null && user.getQuotaUsed() >= user.getQuotaTotal()) {
                throw new IllegalStateException("AI 生成次数已用完，请联系管理员充值。");
            }
            throw new IllegalStateException("您没有使用该 AI 功能的权限，请联系管理员。");
        }
    }

    private void logAi(Long userId, Long productId, String taskType, String platform,
                       String model, String status, String input, String error, long start) {
        AiGenerationLog log = new AiGenerationLog();
        log.setUserId(userId);
        log.setProductId(productId);
        log.setTaskType(taskType);
        log.setPlatform(platform);
        log.setModel(model);
        log.setStatus(status);
        log.setInputSummary(input != null && input.length() > 500 ? input.substring(0, 500) : input);
        log.setErrorMessage(error);
        log.setDurationMs((int) (System.currentTimeMillis() - start));
        log.setCreatedAt(LocalDateTime.now());
        aiGenerationLogMapper.insert(log);
    }
}
