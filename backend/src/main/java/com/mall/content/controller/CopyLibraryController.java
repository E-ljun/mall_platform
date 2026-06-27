package com.mall.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.content.common.ApiResponse;
import com.mall.content.domain.entity.CopyLibrary;
import com.mall.content.service.CopyLibraryService;
import com.mall.content.service.ExportService;
import com.mall.content.domain.entity.MarketingCopy;
import com.mall.content.domain.enums.MarketingPlatform;
import com.mall.content.mapper.MarketingCopyMapper;
import com.mall.content.service.ProductService;
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
public class CopyLibraryController {

    private final CopyLibraryService copyLibraryService;
    private final ExportService exportService;
    private final MarketingCopyMapper marketingCopyMapper;
    private final ProductService productService;

    // ==================== CRUD ====================

    @GetMapping("/library")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String groupName,
            Authentication auth
    ) {
        Long userId = (Long) auth.getPrincipal();
        Page<CopyLibrary> result = copyLibraryService.list(userId, page, size, platform, keyword, groupName);
        return ApiResponse.ok(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @GetMapping("/library/groups")
    public ApiResponse<List<String>> listGroups(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(copyLibraryService.listGroups(userId));
    }

    @PostMapping("/library")
    public ApiResponse<CopyLibrary> collect(@RequestBody CollectRequest req, Authentication auth) {
        return ApiResponse.ok(copyLibraryService.collectFromCopy(req.getCopyId(), req.getGroupName(), auth));
    }

    @PutMapping("/library/{id}")
    public ApiResponse<CopyLibrary> update(@PathVariable Long id, @RequestBody UpdateRequest req, Authentication auth) {
        return ApiResponse.ok(copyLibraryService.update(id, req.getTitle(), req.getContent(), req.getGroupName(), auth));
    }

    @PutMapping("/library/move-group")
    public ApiResponse<Void> moveGroup(@RequestBody MoveGroupRequest req, Authentication auth) {
        copyLibraryService.moveGroup(req.getIds(), req.getGroupName(), auth);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/library/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) {
        copyLibraryService.delete(id, auth);
        return ApiResponse.ok(null);
    }

    // ==================== 导出 ====================

    @GetMapping("/library/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id,
                                          @RequestParam(defaultValue = "txt") String format,
                                          Authentication auth) {
        CopyLibrary lib = copyLibraryService.getById(id, auth);
        MarketingCopy copy = new MarketingCopy();
        copy.setId(id);
        copy.setPlatform(lib.getPlatform());
        copy.setTitle(lib.getTitle());
        copy.setContent(lib.getContent());
        copy.setHashtags(lib.getTags());

        byte[] body; String ext; MediaType ct;
        switch (format.toLowerCase()) {
            case "md":
                body = exportService.exportCopyAsMarkdown(copy); ext = "md"; ct = MediaType.TEXT_PLAIN; break;
            case "pdf":
                body = exportService.exportCopyAsPdf(copy); ext = "pdf"; ct = MediaType.APPLICATION_PDF; break;
            default:
                body = exportService.exportCopyAsTxt(copy); ext = "txt"; ct = MediaType.TEXT_PLAIN;
        }
        String filename = URLEncoder.encode("library-" + id + "." + ext, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(ct).body(body);
    }

    @PostMapping("/library/export-batch")
    public ResponseEntity<byte[]> exportBatch(@RequestBody BatchExportRequest req, Authentication auth) {
        byte[] body; String filename; MediaType ct;
        if ("xlsx".equalsIgnoreCase(req.getFormat())) {
            body = exportService.exportCopiesBatchExcel(req.getIds());
            filename = URLEncoder.encode("library-batch.xlsx", StandardCharsets.UTF_8);
            ct = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            body = exportService.exportCopiesBatchTxt(req.getIds());
            filename = URLEncoder.encode("library-batch.txt", StandardCharsets.UTF_8);
            ct = MediaType.TEXT_PLAIN;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(ct).body(body);
    }

    // ==================== 分组管理 ====================

    @PostMapping("/library/groups")
    public ApiResponse<Void> createGroup(@RequestBody CreateGroupRequest req, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        copyLibraryService.createGroup(userId, req.getGroupName());
        return ApiResponse.ok(null);
    }

    @PutMapping("/library/groups/rename")
    public ApiResponse<Void> renameGroup(@RequestBody RenameGroupRequest req, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        copyLibraryService.renameGroup(userId, req.getOldName(), req.getNewName());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/library/groups/{groupName}")
    public ApiResponse<Map<String, Object>> deleteGroup(@PathVariable String groupName, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        int deleted = copyLibraryService.deleteGroup(userId, groupName);
        return ApiResponse.ok(Map.of("deleted", deleted, "groupName", groupName));
    }

    @Data public static class CollectRequest { private Long copyId; private String groupName; }
    @Data public static class UpdateRequest { private String title; private String content; private String groupName; }
    @Data public static class MoveGroupRequest { private List<Long> ids; private String groupName; }
    @Data public static class BatchExportRequest { private List<Long> ids; private String format; }
    @Data public static class RenameGroupRequest { private String oldName; private String newName; }
    @Data public static class CreateGroupRequest { private String groupName; }
}
