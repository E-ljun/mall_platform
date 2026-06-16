package com.mall.content.service;

import com.mall.content.config.MallProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MallProperties properties;

    public String saveProductImage(Long productId, MultipartFile file) throws IOException {
        validateImage(file);
        Path dir = Paths.get(properties.getStorage().getRoot(), "uploads", String.valueOf(productId));
        Files.createDirectories(dir);
        String safeName = UUID.randomUUID() + "_" + sanitizeFileName(file.getOriginalFilename());
        Path target = dir.resolve(safeName);
        Files.copy(file.getInputStream(), target);
        return target.toString().replace("\\", "/");
    }

    public byte[] readFile(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException ex) {
            throw new IllegalStateException("读取文件失败: " + filePath, ex);
        }
    }

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException ex) {
            throw new IllegalStateException("删除文件失败: " + filePath, ex);
        }
    }

    public void deleteProductDirectory(Long productId) {
        Path dir = Paths.get(properties.getStorage().getRoot(), "uploads", String.valueOf(productId));
        try {
            if (Files.exists(dir)) {
                try (var stream = Files.walk(dir)) {
                    stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("删除商品图片目录失败", ex);
        }
    }

    public String toPublicUrl(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        String root = Paths.get(properties.getStorage().getRoot()).toAbsolutePath().normalize().toString().replace("\\", "/");
        String normalized = Paths.get(filePath).toAbsolutePath().normalize().toString().replace("\\", "/");
        if (!normalized.startsWith(root)) {
            return properties.getStorage().getPublicUrlPrefix() + "/" + Paths.get(filePath).getFileName();
        }
        String relative = normalized.substring(root.length());
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return properties.getStorage().getPublicUrlPrefix() + "/" + relative;
    }

    /**
     * 将 AI 生成的图片字节直接写入商品目录，返回文件路径。
     */
    public String saveGeneratedImage(Long productId, byte[] imageBytes, String fileName) {
        try {
            Path dir = Paths.get(properties.getStorage().getRoot(), "uploads", String.valueOf(productId));
            Files.createDirectories(dir);
            String safeName = UUID.randomUUID() + "_" + sanitizeFileName(fileName);
            Path target = dir.resolve(safeName);
            Files.write(target, imageBytes);
            return target.toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new IllegalStateException("保存生成图片失败: " + fileName, ex);
        }
    }

    public String resolveMimeType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > properties.getStorage().getMaxImageSizeBytes()) {
            throw new IllegalArgumentException("单张图片不能超过 5MB");
        }
        String contentType = file.getContentType();
        boolean allowed = false;
        for (String mime : properties.getStorage().getAllowedMimeTypes()) {
            if (mime.equals(contentType)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new IllegalArgumentException("仅支持 JPG/PNG/WebP 格式");
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "image.jpg";
        return name.replaceAll("[^a-zA-Z0-9._\\u4e00-\\u9fa5-]", "_");
    }
}
