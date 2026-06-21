package com.mall.content.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.content.config.MallProperties;
import com.mall.content.config.MallProperties.AiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AiChatClient {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final MallProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = createRestTemplate();

    public String generateStructuredJson(
            String providerName,
            String model,
            String systemPrompt,
            String userPrompt,
            List<String> imageBase64List
    ) {
        AiProvider provider = properties.getAi().requireProvider(providerName);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.4);
        body.put("response_format", Map.of("type", "json_object"));

        List<Object> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "text", "text", userPrompt));
        for (String image : imageBase64List) {
            userContent.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", image.startsWith("data:") ? image : "data:image/jpeg;base64," + image)
            ));
        }

        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ));

        String raw = callChatCompletions(provider, body);
        return extractJsonBlock(raw);
    }

    public <T> T parseJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception ex) {
            throw new IllegalStateException("AI 返回 JSON 解析失败: " + ex.getMessage(), ex);
        }
    }

    public String extractJsonBlock(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        Matcher matcher = JSON_BLOCK.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        int first = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return trimmed.substring(first, last + 1);
        }
        return trimmed;
    }

    private String callChatCompletions(AiProvider provider, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(provider.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String baseUrl = provider.getBaseUrl().replaceAll("/+$", "");
        String url = baseUrl.endsWith("/v1") ? baseUrl + "/chat/completions" : baseUrl + "/v1/chat/completions";

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<?, ?> responseBody = restTemplate.postForObject(url, entity, Map.class);
        if (responseBody == null) {
            throw new IllegalStateException("AI 接口无响应，请检查 API Key 与网络。");
        }
        List<?> choices = (List<?>) responseBody.get("choices");
        Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        return String.valueOf(message.get("content"));
    }

    /**
     * 调用 DashScope Wan 2.7 图像生成 API（同步，multimodal-generation 端点）。
     * Wan 2.7 使用 multimodal-generation/generation 端点，同步返回结果。
     * @param productImageBase64List 商品参考图（base64 data URI），帮助 Wan 2.7 理解商品外观
     */
    public String generateImage(String providerName, String model, String prompt, String size, int n,
                                List<String> productImageBase64List) {
        AiProvider provider = properties.getAi().requireProvider(providerName);
        String apiKey = provider.getApiKey();

        // Wan 2.7 使用 multimodal-generation 端点
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

        // 尺寸转换：前端传 "3:4" 等，转换为 wan2.7 支持的格式
        String wanSize = convertSize(size);

        // 构建 content 数组：先放商品参考图，再放文字 prompt
        List<Map<String, Object>> contentList = new ArrayList<>();
        if (productImageBase64List != null) {
            for (String imgBase64 : productImageBase64List) {
                contentList.add(Map.of("image", imgBase64));
            }
        }
        contentList.add(Map.of("text", prompt));

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", contentList
        );

        Map<String, Object> input = Map.of("messages", List.of(userMessage));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("size", wanSize);
        parameters.put("n", n);
        parameters.put("watermark", false);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", input);
        body.put("parameters", parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
        if (response == null) {
            throw new IllegalStateException("图像生成接口无响应，请检查 API Key 与网络。");
        }
        if (response.get("code") != null) {
            throw new IllegalStateException("图像生成错误: " + response.get("code") + " - " + response.get("message"));
        }

        // 解析同步响应: output.choices[0].message.content[0].image
        Map<?, ?> output = (Map<?, ?>) response.get("output");
        if (output == null) {
            throw new IllegalStateException("图像生成返回为空。");
        }
        List<?> choices = (List<?>) output.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("图像生成无结果。");
        }
        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        if (message == null) {
            throw new IllegalStateException("图像生成消息为空。");
        }
        List<?> resultContent = (List<?>) message.get("content");
        if (resultContent == null || resultContent.isEmpty()) {
            throw new IllegalStateException("图像生成内容为空。");
        }
        Map<?, ?> content = (Map<?, ?>) resultContent.get(0);
        String imageUrl = String.valueOf(content.get("image"));
        if (imageUrl == null || imageUrl.isBlank() || "null".equals(imageUrl)) {
            throw new IllegalStateException("图像生成返回空 URL。");
        }

        // 下载图片并返回 base64（用 URLConnection 避免 OSS 签名问题）
        try {
            byte[] imageBytes = new java.net.URL(imageUrl).openStream().readAllBytes();
            if (imageBytes != null && imageBytes.length > 0) {
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception downloadEx) {
            // 下载失败时返回原始 URL，让前端直接引用（OSS URL 有效期约 24h）
        }
        return imageUrl;
    }

    /**
     * 将前端传入的宽高比转换为 Wan 2.7 支持的尺寸格式。
     * Wan 2.7 支持: "1K", "2K", "4K"
     */
    private String convertSize(String size) {
        if (size == null) return "2K";
        // 如果已经是 Wan 格式，直接返回
        if (size.equals("1K") || size.equals("2K") || size.equals("4K")) {
            return size;
        }
        // 前端传入的是宽高比，如 "3:4", "1:1", "16:9", "9:16"
        // 统一用 2K 品质，通过 prompt 中的 aspectRatio 来控制宽高比
        return "2K";
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(120000);
        return new RestTemplate(factory);
    }
}
