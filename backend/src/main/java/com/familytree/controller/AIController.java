package com.familytree.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@SuppressWarnings("unchecked")
public class AIController {

    @Value("${ai.api-key:sk-b992d8a187454f61a9ec28d2a222c4e2}")
    private String apiKey;

    @Value("${ai.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    @PostMapping("/query-relationship")
    public ResponseEntity<Map<String, Object>> queryRelationship(@RequestBody Map<String, String> body) {
        String label = body.get("label");
        if (label == null || label.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请提供关系名称"));
        }

        String prompt = "你是一个中国家族关系专家。请用一段话（100字左右）介绍「" + label + "」这个家族关系称谓的含义、所属类别（直系/旁系/姻亲）以及在家族中的角色。语言通俗易懂。";

        try {
            RestTemplate rest = new RestTemplate();

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "max_tokens", 300
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = rest.postForEntity(apiUrl, request, (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getBody() != null) {
                Map<String, Object> choices = (Map<String, Object>) ((List<Object>) response.getBody().get("choices")).get(0);
                Map<String, Object> message = (Map<String, Object>) choices.get("message");
                String content = (String) message.get("content");
                return ResponseEntity.ok(Map.of("result", content.trim()));
            }

            return ResponseEntity.ok(Map.of("result", "无法获取介绍，请稍后重试。"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("result", "AI查询服务暂时不可用，请稍后重试。"));
        }
    }
}
