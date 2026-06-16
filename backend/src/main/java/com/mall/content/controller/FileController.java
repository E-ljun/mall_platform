package com.mall.content.controller;

import com.mall.content.common.ApiResponse;
import com.mall.content.config.MallProperties;
import com.mall.content.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;
    private final MallProperties properties;

    @GetMapping("/**")
    public ResponseEntity<ByteArrayResource> serveFile(HttpServletRequest request) {
        String prefix = request.getContextPath() + "/files/";
        String uri = request.getRequestURI();
        if (!uri.startsWith(prefix)) {
            return ResponseEntity.notFound().build();
        }
        String relative = uri.substring(prefix.length());
        Path root = Paths.get(properties.getStorage().getRoot()).toAbsolutePath().normalize();
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root) || !file.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = storageService.readFile(file.toString());
        String mime = storageService.resolveMimeType(file.toString());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.parseMediaType(mime))
                .body(new ByteArrayResource(bytes));
    }
}
