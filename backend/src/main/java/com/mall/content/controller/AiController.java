package com.mall.content.controller;

import com.mall.content.ai.dto.MarketingCopyResult;
import com.mall.content.ai.dto.ProductDescriptionResult;
import com.mall.content.common.ApiResponse;
import com.mall.content.domain.entity.MarketingCopy;
import com.mall.content.domain.entity.ProductImage;
import com.mall.content.domain.enums.MarketingPlatform;
import com.mall.content.mapper.MarketingCopyMapper;
import com.mall.content.service.AiContentService;
import com.mall.content.service.ExportService;
import com.mall.content.service.ProductService;
import com.mall.content.service.StorageService;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AiController {

    private final AiContentService aiContentService;
    private final ExportService exportService;
    private final MarketingCopyMapper marketingCopyMapper;
    private final ProductService productService;
    private final StorageService storageService;

    @PostMapping("/ai/products/{productId}/description")
    public ApiResponse<ProductDescriptionResult> generateDescription(
            @PathVariable Long productId,
            @RequestBody DescriptionRequest request,
            Authentication auth
    ) {
        return ApiResponse.ok(aiContentService.generateProductDescription(
                productId, auth, request.getImageIds(), request.getKeywords()
        ));
    }

    @PostMapping("/ai/products/{productId}/marketing-copy")
    public ApiResponse<MarketingCopyResult> generateMarketingCopy(
            @PathVariable Long productId,
            @RequestBody MarketingCopyRequest request,
            Authentication auth
    ) {
        MarketingPlatform platform = MarketingPlatform.valueOf(request.getPlatform().toUpperCase());
        int variantCount = request.getVariantCount() == null ? 3 : request.getVariantCount();
        return ApiResponse.ok(aiContentService.generateMarketingCopy(productId, auth, platform, variantCount));
    }

    @PostMapping("/ai/products/{productId}/detail-image")
    public ApiResponse<Map<String, Object>> generateDetailImage(
            @PathVariable Long productId,
            @RequestBody DetailImageRequest request,
            Authentication auth
    ) {
        ProductImage image = aiContentService.generateDetailImage(
                productId, auth,
                request.getSectionTitle(), request.getSectionCopy(),
                request.getVisualDirection(), request.getAspectRatio()
        );
        return ApiResponse.ok(Map.of(
                "id", image.getId(),
                "url", storageService.toPublicUrl(image.getFilePath()),
                "fileName", image.getFileName() != null ? image.getFileName() : ""
        ));
    }

    @GetMapping("/products/{productId}/marketing-copies")
    public ApiResponse<List<MarketingCopy>> listCopies(@PathVariable Long productId, Authentication auth) {
        return ApiResponse.ok(aiContentService.listCopies(productId, auth));
    }

    @PutMapping("/marketing-copies/{copyId}")
    public ApiResponse<MarketingCopy> updateCopy(
            @PathVariable Long copyId,
            @RequestBody CopyUpdateRequest request,
            Authentication auth
    ) {
        return ApiResponse.ok(aiContentService.updateCopy(copyId, request.getTitle(), request.getContent(), auth));
    }

    @PostMapping("/marketing-copies/{copyId}/favorite")
    public ApiResponse<MarketingCopy> favorite(@PathVariable Long copyId, Authentication auth) {
        return ApiResponse.ok(aiContentService.toggleFavorite(copyId, auth));
    }

    @DeleteMapping("/marketing-copies/{copyId}")
    public ApiResponse<Void> deleteCopy(@PathVariable Long copyId, Authentication auth) {
        aiContentService.deleteCopy(copyId, auth);
        return ApiResponse.ok(null);
    }

    @GetMapping("/marketing-copies/{copyId}/export")
    public ResponseEntity<byte[]> exportCopy(
            @PathVariable Long copyId,
            @RequestParam(defaultValue = "txt") String format,
            Authentication auth
    ) {
        MarketingCopy copy = marketingCopyMapper.selectById(copyId);
        if (copy == null) {
            throw new IllegalArgumentException("文案不存在");
        }
        productService.requireAccessibleProduct(copy.getProductId(), auth);

        byte[] body;
        String ext;
        MediaType contentType;

        switch (format.toLowerCase()) {
            case "md":
                body = exportService.exportCopyAsMarkdown(copy);
                ext = "md";
                contentType = MediaType.TEXT_PLAIN;
                break;
            case "pdf":
                body = exportService.exportCopyAsPdf(copy);
                ext = "pdf";
                contentType = MediaType.APPLICATION_PDF;
                break;
            default:
                body = exportService.exportCopyAsTxt(copy);
                ext = "txt";
                contentType = MediaType.TEXT_PLAIN;
                break;
        }

        String filename = URLEncoder.encode("copy-" + copyId + "." + ext, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(contentType)
                .body(body);
    }

    @PostMapping("/marketing-copies/export-batch")
    public ResponseEntity<byte[]> exportBatch(@RequestBody BatchExportRequest request, Authentication auth) {
        for (Long id : request.getIds()) {
            MarketingCopy copy = marketingCopyMapper.selectById(id);
            if (copy != null) {
                productService.requireAccessibleProduct(copy.getProductId(), auth);
            }
        }

        byte[] body;
        String filename;
        MediaType contentType;

        if ("xlsx".equalsIgnoreCase(request.getFormat())) {
            body = exportService.exportCopiesBatchExcel(request.getIds());
            filename = URLEncoder.encode("copies-batch.xlsx", StandardCharsets.UTF_8);
            contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            body = exportService.exportCopiesBatchTxt(request.getIds());
            filename = URLEncoder.encode("copies-batch.txt", StandardCharsets.UTF_8);
            contentType = MediaType.TEXT_PLAIN;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(contentType)
                .body(body);
    }

    @Data
    public static class DescriptionRequest {
        @NotEmpty
        private List<Long> imageIds;
        private String keywords;
    }

    @Data
    public static class MarketingCopyRequest {
        private String platform;
        private Integer variantCount;
    }

    @Data
    public static class CopyUpdateRequest {
        private String title;
        private String content;
    }

    @Data
    public static class DetailImageRequest {
        private String sectionTitle;
        private String sectionCopy;
        private String visualDirection;
        private String aspectRatio;
    }

    /**
     * 一键流水线：按顺序执行"商品描述 → 营销文案 → 详情图"。
     * 前一步的输出作为后一步的输入，支持任意组合。
     */
    @PostMapping("/ai/products/{productId}/pipeline")
    public ApiResponse<Map<String, Object>> pipeline(
            @PathVariable Long productId,
            @RequestBody PipelineRequest request,
            Authentication auth
    ) {
        Map<String, Object> results = new java.util.LinkedHashMap<>();
        List<String> steps = request.getSteps();
        if (steps == null || steps.isEmpty()) {
            return ApiResponse.fail("BAD_REQUEST", "请至少选择一个生成步骤");
        }

        // Step 1: 商品描述
        ProductDescriptionResult desc = null;
        if (steps.contains("description")) {
            desc = aiContentService.generateProductDescription(
                    productId, auth, request.getImageIds(), request.getKeywords());
            results.put("description", desc);
        }

        // Step 2: 营销文案（可基于刚生成的描述）
        if (steps.contains("copy")) {
            MarketingPlatform platform = request.getPlatform() != null
                    ? MarketingPlatform.valueOf(request.getPlatform().toUpperCase())
                    : MarketingPlatform.XIAOHONGSHU;
            int vc = request.getVariantCount() != null ? request.getVariantCount() : 3;
            MarketingCopyResult copyResult = aiContentService.generateMarketingCopy(
                    productId, auth, platform, vc);
            results.put("copy", copyResult);
            results.put("copyPlatform", platform.name());
        }

        // Step 3: 详情图（参考商品图片）
        if (steps.contains("image")) {
            ProductImage image = aiContentService.generateDetailImage(
                    productId, auth,
                    request.getImageSectionTitle(),
                    request.getImageSectionCopy(),
                    request.getImageVisualDirection(),
                    request.getImageAspectRatio()
            );
            results.put("image", Map.of(
                    "id", image.getId(),
                    "url", storageService.toPublicUrl(image.getFilePath()),
                    "fileName", image.getFileName() != null ? image.getFileName() : ""
            ));
        }

        return ApiResponse.ok(results);
    }

    @Data
    public static class PipelineRequest {
        @NotEmpty
        private List<String> steps; // description | copy | image
        private List<Long> imageIds;
        private String keywords;
        private String platform;   // XIAOHONGSHU | TAOBAO | DOUYIN
        private Integer variantCount;
        private String imageSectionTitle;
        private String imageSectionCopy;
        private String imageVisualDirection;
        private String imageAspectRatio;
    }

    @Data
    public static class BatchExportRequest {
        @NotEmpty
        private List<Long> ids;
        private String format;
    }
}
