package com.example.menumaker.service;

import com.example.menumaker.client.BedrockClient;
import com.example.menumaker.dto.AiCandidate;
import com.example.menumaker.dto.Candidate;
import com.example.menumaker.dto.RecipeLink;
import com.example.menumaker.dto.SuggestionResponse;
import com.example.menumaker.exception.AiResponseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class SuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);

    private final BedrockClient bedrockClient;
    private final ObjectMapper objectMapper;

    public SuggestionService(BedrockClient bedrockClient, ObjectMapper objectMapper) {
        this.bedrockClient = bedrockClient;
        this.objectMapper = objectMapper;
    }

    public SuggestionResponse suggest(List<String> ingredients) {
        String prompt = buildPrompt(ingredients);
        List<AiCandidate> aiCandidates = tryFetchCandidates(prompt);
        if (aiCandidates == null) {
            aiCandidates = tryFetchCandidates(prompt);
        }
        if (aiCandidates == null) {
            throw new AiResponseException("failed to parse AI response after retry");
        }

        List<Candidate> candidates = aiCandidates.stream()
                .map(candidate -> new Candidate(
                        candidate.name(),
                        candidate.description(),
                        candidate.calories(),
                        candidate.fat(),
                        candidate.sugar(),
                        candidate.carbs(),
                        buildRecipeLinks(candidate.name())))
                .toList();
        return new SuggestionResponse(candidates);
    }

    private List<AiCandidate> tryFetchCandidates(String prompt) {
        try {
            String requestBody = buildRequestBody(prompt);
            String rawResponse = bedrockClient.invoke(requestBody);
            String text = extractText(rawResponse);
            List<AiCandidate> candidates = objectMapper.readValue(text, new TypeReference<List<AiCandidate>>() {
            });
            candidates.forEach(SuggestionService::requireAllFields);
            return candidates;
        } catch (Exception e) {
            log.warn("failed to fetch/parse AI candidates", e);
            return null;
        }
    }

    /** 項目が欠けた応答は不正とみなし、リトライ対象にする。 */
    private static void requireAllFields(AiCandidate candidate) {
        if (candidate == null
                || candidate.name() == null
                || candidate.description() == null
                || candidate.calories() == null
                || candidate.fat() == null
                || candidate.sugar() == null
                || candidate.carbs() == null) {
            throw new IllegalArgumentException("AI candidate has missing fields");
        }
    }

    private String buildRequestBody(String prompt) throws JsonProcessingException {
        Map<String, Object> body = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 1024,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        return objectMapper.writeValueAsString(body);
    }

    private String extractText(String rawResponse) throws JsonProcessingException {
        String text = objectMapper.readTree(rawResponse).at("/content/0/text").asText();
        return stripCodeFence(text);
    }

    /**
     * Claudeはプロンプトで禁止していても```json ... ```のようにコードフェンスで
     * 応答を囲むことがあるため、JSON配列の実体部分だけを取り出す。
     */
    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start == -1 || end == -1 || end < start) {
            return trimmed;
        }
        return trimmed.substring(start, end + 1);
    }

    private String buildPrompt(List<String> ingredients) {
        String joined = String.join(", ", ingredients);
        return "食材: " + joined + "。この食材で作れる料理を5つ提案してください。"
                + "出力は説明文を一切含めず、name・description・calories・fat・sugar・carbsのみを持つJSON配列のみにしてください。"
                + "calories(1人前のkcal)・fat(脂質g)・sugar(糖質g)・carbs(炭水化物g)はおおよその概算値を整数で入れてください。"
                + "例: [{\"name\":\"料理名\",\"description\":\"簡単な説明\",\"calories\":450,\"fat\":15,\"sugar\":40,\"carbs\":55}]";
    }

    /**
     * 特定レシピへの直接リンクは公式APIがないため、料理名で複数の
     * 大手レシピサイトのサイト内検索に飛ばす。1サイトの検索精度に
     * 依存しないよう候補を複数用意する。
     */
    private List<RecipeLink> buildRecipeLinks(String dishName) {
        String encoded = URLEncoder.encode(dishName, StandardCharsets.UTF_8);
        return List.of(
                new RecipeLink("cookpad", "クックパッドで探す", "https://cookpad.com/jp/search/" + encoded),
                new RecipeLink("kurashiru", "クラシルで探す", "https://www.kurashiru.com/search?query=" + encoded),
                new RecipeLink("rakuten-recipe", "楽天レシピで探す", "https://recipe.rakuten.co.jp/search/" + encoded + "/")
        );
    }
}
