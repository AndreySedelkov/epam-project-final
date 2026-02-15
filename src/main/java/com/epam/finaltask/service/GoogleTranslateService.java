package com.epam.finaltask.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleTranslateService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.translate.google.api-key:}")
    private String apiKey;

    @Value("${app.translate.google.enabled:true}")
    private boolean enabled;

    public String translate(String text, String targetLanguage) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return null;
        }
        if (text == null || text.isBlank()) {
            return null;
        }

        String url = "https://translation.googleapis.com/language/translate/v2?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of(
                "q", text,
                "target", targetLanguage,
                "format", "text"
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
        if (response == null) {
            return null;
        }

        Object data = response.get("data");
        if (!(data instanceof Map)) {
            return null;
        }
        Object translations = ((Map<?, ?>) data).get("translations");
        if (!(translations instanceof List) || ((List<?>) translations).isEmpty()) {
            return null;
        }
        Object first = ((List<?>) translations).get(0);
        if (!(first instanceof Map)) {
            return null;
        }
        Object translatedText = ((Map<?, ?>) first).get("translatedText");
        if (!(translatedText instanceof String)) {
            return null;
        }
        return HtmlUtils.htmlUnescape((String) translatedText);
    }
}
