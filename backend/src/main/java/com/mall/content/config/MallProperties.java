package com.mall.content.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "mall")
public class MallProperties {

    private Jwt jwt = new Jwt();
    private Storage storage = new Storage();
    private Cors cors = new Cors();
    private Ai ai = new Ai();
    private Pdf pdf = new Pdf();

    @Data
    public static class Jwt {
        private String secret;
        private int expireHours = 24;
    }

    @Data
    public static class Storage {
        private String root = "./storage";
        private String publicUrlPrefix = "/api/files";
        private String[] allowedMimeTypes = {"image/jpeg", "image/png", "image/webp"};
        private int maxImagesPerProduct = 10;
        private long maxImageSizeBytes = 5 * 1024 * 1024;
    }

    @Data
    public static class Cors {
        private String allowedOriginPatterns = "http://localhost:*";
    }

    @Data
    public static class Ai {
        private String defaultProvider = "dashscope";
        private Map<String, AiProvider> providers = new HashMap<>();
        private Routing routing = new Routing();
        private long timeoutMs = 90000;

        public AiProvider requireProvider(String name) {
            AiProvider provider = providers.get(name);
            if (provider == null || provider.getApiKey() == null || provider.getApiKey().isBlank()) {
                provider = providers.get(defaultProvider);
            }
            if (provider == null || provider.getApiKey() == null || provider.getApiKey().isBlank()) {
                throw new IllegalStateException("未配置 AI API Key，请设置 DASHSCOPE_API_KEY 环境变量。");
            }
            return provider;
        }
    }

    @Data
    public static class AiProvider {
        private String baseUrl;
        private String apiKey;
        private String visionModel;
        private String textModel;
        private String imageModel;
    }

    @Data
    public static class Pdf {
        private String fontPaths = "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc,"
                + "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,"
                + "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,"
                + "C:/Windows/Fonts/msyh.ttc,"
                + "C:/Windows/Fonts/simsun.ttc";
        private String fontFamily = "Noto Sans CJK SC";
    }

    @Data
    public static class Routing {
        private String productDescription = "dashscope";
        private String marketingCopy = "dashscope";
        private String fallbackText = "deepseek";
    }
}
