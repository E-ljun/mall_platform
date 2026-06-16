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
     * 调用图像生成 API（DashScope Wan 2.7 等），返回 base64 图片数据。
     */
    public String generateImage(String providerName, String model, String prompt, String size, int n) {
        AiProvider provider = properties.getAi().requireProvider(providerName);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("n", n);
        body.put("size", size != null ? size : "1024x1024");
        body.put("response_format", "b64_json");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(provider.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String baseUrl = provider.getBaseUrl().replaceAll("/+$", "");
        String url = baseUrl.endsWith("/v1") ? baseUrl + "/images/generations" : baseUrl + "/v1/images/generations";

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<?, ?> responseBody = restTemplate.postForObject(url, entity, Map.class);
        if (responseBody == null) {
            throw new IllegalStateException("图像生成接口无响应，请检查 API Key 与网络。");
        }
        List<?> data = (List<?>) responseBody.get("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("图像生成返回数据为空。");
        }
        Map<?, ?> first = (Map<?, ?>) data.get(0);
        Object b64 = first.get("b64_json");
        if (b64 != null) {
            return b64.toString();
        }
        Object urlVal = first.get("url");
        return urlVal != null ? urlVal.toString() : "";
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(120000);
        return new RestTemplate(factory);
    }
}
