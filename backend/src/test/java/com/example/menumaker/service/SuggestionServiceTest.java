package com.example.menumaker.service;

import com.example.menumaker.client.BedrockClient;
import com.example.menumaker.dto.Candidate;
import com.example.menumaker.exception.AiResponseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SuggestionService のテスト(全10問)。
 *
 * ■ このテストは何を確かめるのか
 *   SuggestionService は「食材 → AI(Bedrock)に問い合わせ → 返ってきたJSONを解釈 → 料理候補を返す」クラス。
 *   ただしテストでは本物のAIは呼ばない(遅い・お金がかかる・毎回答えが変わるため)。
 *   代わりに BedrockClient を「モック(偽物)」に差し替え、
 *   「AIがこう返してきたら、Serviceはこう動くはず」を確認する。
 *
 * ■ テストの基本形(AAA)
 *   準備(Arrange): モックに「こう返せ」と指示する
 *   実行(Act)    : 試したいメソッドを呼ぶ
 *   検証(Assert) : 結果が期待通りか assertThat(...) で確かめる
 */
class SuggestionServiceTest {

    private final BedrockClient bedrockClient = Mockito.mock(BedrockClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SuggestionService service = new SuggestionService(bedrockClient, objectMapper);

    // ==== テスト用の道具 ==========================================================

    /** AIが返す「料理5件のJSON配列」のサンプル。1件目は「オムライス」、calories は 450。 */
    private static final String FIVE_DISHES_JSON = """
            [
              {"name":"オムライス","description":"卵とご飯","calories":450,"fat":15,"sugar":40,"carbs":55},
              {"name":"玉ねぎのスープ","description":"甘いスープ","calories":120,"fat":3,"sugar":15,"carbs":18},
              {"name":"卵焼き","description":"定番","calories":150,"fat":10,"sugar":2,"carbs":3},
              {"name":"チャーハン","description":"手早く","calories":500,"fat":14,"sugar":60,"carbs":70},
              {"name":"親子丼","description":"甘辛","calories":600,"fat":16,"sugar":75,"carbs":85}
            ]
            """;

    /** Bedrock が実際に返す形式 {"content":[{"type":"text","text":"..."}]} の文字列を作る。 */
    private String bedrockResponse(String text) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("content", List.of(Map.of("type", "text", "text", text))));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** モックに「invoke が呼ばれたら、この応答を(順に)返せ」と指示する。 */
    private void stubBedrock(String... responses) {
        OngoingStubbing<String> stubbing = Mockito.when(bedrockClient.invoke(Mockito.anyString()));
        for (String response : responses) {
            stubbing = stubbing.thenReturn(response);
        }
    }

    /** 料理名をURL用に変換する。期待値を作るのに使う。 */
    private String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    // ==== 例題(解答済み)=========================================================

    @Test
    void 問1_正常系_AIの応答から5件の候補が返る() {
        stubBedrock(bedrockResponse(FIVE_DISHES_JSON));

        var result = service.suggest(List.of("卵", "玉ねぎ"));

        assertThat(result.candidates()).hasSize(5);
        assertThat(result.candidates().get(0).name()).isEqualTo("オムライス");
        assertThat(result.candidates().get(0).calories()).isEqualTo(450);
        assertThat(result.candidates().stream().map(Candidate::name))
                .containsExactly("オムライス", "玉ねぎのスープ", "卵焼き", "チャーハン", "親子丼");
    }

    @Test
    void 問2_異常系_二回とも壊れたJSONならAiResponseExceptionを投げる() {
        stubBedrock(bedrockResponse("これはJSONではありません"));

        assertThrows(AiResponseException.class, () -> service.suggest(List.of("卵")));

        Mockito.verify(bedrockClient, Mockito.times(2)).invoke(Mockito.anyString());
    }

    // ==== 解答 ===================================================================

    /** 問3【易】レシピリンクが、3サイト分、料理名をURLエンコードして作られる。 */
    @Test
    void 問3_レシピリンク_3サイト分が料理名をURLエンコードして作られる() {
        // 準備
        stubBedrock(bedrockResponse(FIVE_DISHES_JSON));

        // 実行: 1件目(オムライス)のリンク一覧を取り出す
        var links = service.suggest(List.of("卵")).candidates().get(0).recipeLinks();

        // 検証その1: サイト名が順番通り
        assertThat(links.stream().map(link -> link.site()))
                .containsExactly("cookpad", "kurashiru", "rakuten-recipe");

        // 検証その2: URLが料理名をエンコードして作られている
        String encoded = encode("オムライス");
        assertThat(links.stream().map(link -> link.url()))
                .containsExactly(
                        "https://cookpad.com/jp/search/" + encoded,
                        "https://www.kurashiru.com/search?query=" + encoded,
                        "https://recipe.rakuten.co.jp/search/" + encoded + "/");
    }

    /** 問4【易】AIがコードフェンス(```json ... ```)で囲んで返しても、パースできる。 */
    @Test
    void 問4_コードフェンス付きの応答でもパースできる() {
        // 準備: JSONをコードフェンスで囲む
        String fenced = "```json\n" + FIVE_DISHES_JSON + "\n```";
        stubBedrock(bedrockResponse(fenced));

        // 実行
        var result = service.suggest(List.of("卵"));

        // 検証: 例外にならず5件返る = フェンスを除去してパースできた
        assertThat(result.candidates()).hasSize(5);
        // 念のため1回目で成功していること(リトライで救われたのではないこと)も確認
        Mockito.verify(bedrockClient, Mockito.times(1)).invoke(Mockito.anyString());
    }

    /** 問5【易】JSONの前後に説明文がついていても、パースできる。 */
    @Test
    void 問5_前置きの文章付きの応答でもパースできる() {
        // 準備: 前後に説明文を足す
        String withText = "以下が候補です。\n" + FIVE_DISHES_JSON + "\n以上です。";
        stubBedrock(bedrockResponse(withText));

        // 実行
        var result = service.suggest(List.of("卵"));

        // 検証
        assertThat(result.candidates()).hasSize(5);
        assertThat(result.candidates().get(0).name()).isEqualTo("オムライス");
    }

    /** 問6【中】1回目が壊れたJSONでも、2回目で成功すれば結果を返す(リトライの確認)。 */
    @Test
    void 問6_一回目が壊れたJSONでも二回目で成功すれば結果を返す() {
        // 準備: 1回目は壊れた応答、2回目は正常な応答
        stubBedrock(
                bedrockResponse("これはJSONではありません"),   // 1回目
                bedrockResponse(FIVE_DISHES_JSON));            // 2回目

        // 実行
        var result = service.suggest(List.of("卵"));

        // 検証その1: 5件返る
        assertThat(result.candidates()).hasSize(5);
        // 検証その2: AIがちょうど2回呼ばれた = 1回目失敗 → リトライで成功
        Mockito.verify(bedrockClient, Mockito.times(2)).invoke(Mockito.anyString());
    }

    /** 問7【中】入力した食材が、AIに送るプロンプトに含まれる。 */
    @Test
    void 問7_プロンプトに入力した食材が含まれる() throws Exception {
        // 準備: 渡された引数を保存する入れ物
        AtomicReference<String> requestBody = new AtomicReference<>("");
        Mockito.when(bedrockClient.invoke(Mockito.anyString())).thenAnswer(invocation -> {
            requestBody.set(invocation.getArgument(0));      // 送られたJSON文字列を保存
            return bedrockResponse(FIVE_DISHES_JSON);        // 戻り値は正常な応答
        });

        // 実行
        service.suggest(List.of("卵", "玉ねぎ"));

        // 検証: リクエストJSONからプロンプト本文を取り出し、食材が含まれることを確認
        String prompt = objectMapper.readTree(requestBody.get())
                .at("/messages/0/content").asText();
        assertThat(prompt).contains("卵").contains("玉ねぎ");
    }

    /**
     * 問8【考える】AIが5件未満(例: 3件)を返したら、今の実装はどう動く?
     *
     * 予想と結論: 実装には件数チェックが無いので、エラーにはならず3件のまま返る。
     *   リトライも起きない(AI呼び出しは1回)。
     *
     * もし「必ず5件でなければエラー」にしたい場合:
     *   SuggestionService のパース処理(requireAllFields の近く)に
     *   「件数が5でなければ AiResponseException 相当の失敗として扱う」チェックを足す。
     *   そのときこのテストは、問2と同じく
     *     assertThrows(AiResponseException.class, ...) と verify(times(2))
     *   を確認する形に書き換わる。
     */
    @Test
    void 問8_AIが5件未満を返したときの挙動() {
        // 準備: 3件だけのJSON
        String threeDishes = """
                [
                  {"name":"A","description":"a","calories":1,"fat":1,"sugar":1,"carbs":1},
                  {"name":"B","description":"b","calories":2,"fat":2,"sugar":2,"carbs":2},
                  {"name":"C","description":"c","calories":3,"fat":3,"sugar":3,"carbs":3}
                ]
                """;
        stubBedrock(bedrockResponse(threeDishes));

        // 実行
        var result = service.suggest(List.of("卵"));

        // 検証: 現状の実装では、3件がそのまま返る(エラーにもリトライにもならない)
        assertThat(result.candidates()).hasSize(3);
        assertThat(result.candidates().stream().map(Candidate::name))
                .containsExactly("A", "B", "C");
        Mockito.verify(bedrockClient, Mockito.times(1)).invoke(Mockito.anyString());
    }

    /** 問9【考える】caloriesが数値ではなく文字列(例: "約450")で返ってきたら? */
    @Test
    void 問9_caloriesが数値でない応答は例外になる() {
        // 準備: calories が "約450"(数値に変換できない文字列)
        String invalid = "[{\"name\":\"A\",\"description\":\"a\",\"calories\":\"約450\","
                + "\"fat\":1,\"sugar\":1,\"carbs\":1}]";
        stubBedrock(bedrockResponse(invalid));

        // 実行 + 検証: パース失敗 → リトライ → それでも失敗 → 例外
        assertThrows(AiResponseException.class, () -> service.suggest(List.of("卵")));

        // 検証: リトライしているので2回呼ばれている
        Mockito.verify(bedrockClient, Mockito.times(2)).invoke(Mockito.anyString());
    }

    /** 問10【考える】AIの応答で、項目(例: name)が抜けていたら? */
    @Test
    void 問10_項目が欠けた応答は例外になる() {
        // 準備: name が無いJSON
        String missingName = "[{\"description\":\"a\",\"calories\":1,"
                + "\"fat\":1,\"sugar\":1,\"carbs\":1}]";
        stubBedrock(bedrockResponse(missingName));

        // 実行 + 検証: requireAllFields が欠けを検出 → リトライ → 例外
        assertThrows(AiResponseException.class, () -> service.suggest(List.of("卵")));

        // 検証: AIは2回呼ばれている
        Mockito.verify(bedrockClient, Mockito.times(2)).invoke(Mockito.anyString());
    }
}