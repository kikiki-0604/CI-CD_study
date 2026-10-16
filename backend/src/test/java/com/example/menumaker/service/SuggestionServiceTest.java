package com.example.menumaker.service;

import com.example.menumaker.client.BedrockClient;
import com.example.menumaker.dto.Candidate;
import com.example.menumaker.exception.AiResponseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * SuggestionService のテスト(全10問)。
 *
 * ■ このテストは何を確かめるのか
 *   SuggestionService は「食材 → AI(Bedrock)に問い合わせ → 返ってきたJSONを解釈 → 料理候補を返す」クラス。
 *   ただしテストでは本物のAIは呼ばない(遅い・お金がかかる・毎回答えが変わるため)。
 *   代わりに BedrockClient を「モック(偽物)」に差し替え、
 *   「AIがこう返してきたら、Serviceはこう動くはず」を確認する。
 *
 * ■ 問題の構成
 *   問1 : 例題(正常系)  解答済み。まずこれを読んで書き方をつかむ
 *   問2 : 例題(異常系)  解答済み。例外のテストの書き方をつかむ
 *   問3〜10 : あなたが書く問題。@Disabled と fail("TODO...") の2行を消して実装する
 *
 * ■ 進め方
 *   1. 問1・問2を読む(特に「準備 → 実行 → 検証」の3段構成)
 *   2. 問3から順に、TODO のヒントを読みながら実装する(易しい順に並べてある)
 *   3. 1問書くたびに、そのテストだけ実行して緑になるのを確認する
 *        (実行コマンドは docs/workshop-guide.md のステップ5-1。1問だけ実行するときは、最後に --tests "*SuggestionServiceTest.問3_*" を付ける。問の番号のあとの _ を忘れずに)
 *   4. 実装を見たいときは SuggestionService.java を読む(各問に「見るところ」を書いてある)
 *   5. Java の書き方が分からないときは docs/java-test-primer.md を見る
 *
 * ■ テストの基本形(AAA)
 *   準備(Arrange): モックに「こう返せ」と指示する
 *   実行(Act)    : 試したいメソッドを呼ぶ
 *   検証(Assert) : 結果が期待通りか assertThat(...) で確かめる
 *
 * ■ 使うもの(spring-boot-starter-test に同梱済み)
 *   JUnit5(@Test, assertThrows) / Mockito(モック) / AssertJ(assertThat)
 */
class SuggestionServiceTest {

    private final BedrockClient bedrockClient = Mockito.mock(BedrockClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SuggestionService service = new SuggestionService(bedrockClient, objectMapper);

    // ==== テスト用の道具(自由に使ってよい) =========================================

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

    /**
     * Bedrock が実際に返す形式の文字列を作る。
     * 本物の応答は {"content":[{"type":"text","text":"<AIの出力>"}]} という形で、
     * AIの出力(料理のJSON)は content[0].text の中に入っている。
     * 例: bedrockResponse(FIVE_DISHES_JSON) → 「AIが5件のJSONを返した」状況を再現できる。
     */
    private String bedrockResponse(String text) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("content", List.of(Map.of("type", "text", "text", text))));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * モックに「invoke が呼ばれたら、この応答を返せ」と指示する。
     * 引数を複数渡すと「1回目はこれ、2回目はこれ…」と順に返す。
     * 最後に渡した応答は、3回目以降も返り続ける(= 1つだけ渡せば毎回それが返る)。
     *   stubBedrock(a)       → 毎回 a
     *   stubBedrock(a, b)    → 1回目 a、2回目以降 b
     */
    private void stubBedrock(String... responses) {
        OngoingStubbing<String> stubbing = Mockito.when(bedrockClient.invoke(Mockito.anyString()));
        for (String response : responses) {
            stubbing = stubbing.thenReturn(response);
        }
    }

    /** 料理名をURL用に変換する(日本語 → %E3%82%AA... の形)。期待値を作るのに使う。 */
    private String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    // ==== 例題(解答済み)=========================================================

    /**
     * 問1【例題・正常系】AIが5件の料理を返したら、5件の候補が返る。
     * いちばん基本の形。「準備 → 実行 → 検証」の流れを覚えよう。
     */
    @Test
    void 問1_正常系_AIの応答から5件の候補が返る() {
        // 準備: Bedrockが「5件のJSON」を返す状況を作る。
        //   anyString() は「どんな引数で呼ばれても」の意味。
        stubBedrock(bedrockResponse(FIVE_DISHES_JSON));

        // 実行: テスト対象のメソッドを呼ぶ(食材は何でもよい)
        var result = service.suggest(List.of("卵", "玉ねぎ"));

        // 検証: assertThat(実際の値).<期待する条件>
        assertThat(result.candidates()).hasSize(5);
        assertThat(result.candidates().get(0).name()).isEqualTo("オムライス");
        assertThat(result.candidates().get(0).calories()).isEqualTo(450);
        // 全件の名前を順番込みで確認する書き方。map で「名前だけ」のリストに変換している。
        assertThat(result.candidates().stream().map(Candidate::name))
                .containsExactly("オムライス", "玉ねぎのスープ", "卵焼き", "チャーハン", "親子丼");
    }

    /**
     * 問2【例題・異常系】AIの応答が2回とも壊れていたら、AiResponseException が投げられる。
     * 「例外が出ること」を確かめるときは assertThrows を使う。
     * また「何回AIを呼んだか」を verify で確かめる書き方も覚えよう。
     */
    @Test
    void 問2_異常系_二回とも壊れたJSONならAiResponseExceptionを投げる() {
        // 準備: JSONではない文字列を、毎回返す(stubBedrock は1つ渡せば毎回それが返る)
        stubBedrock(bedrockResponse("これはJSONではありません"));

        // 実行 + 検証: ラムダ式 () -> ... の中で例外が出ることを確認する。
        //   例外が出なければ、このテストは失敗する。
        assertThrows(AiResponseException.class, () -> service.suggest(List.of("卵")));

        // 検証: Serviceは失敗時に1回だけリトライする仕様なので、AIは合計2回呼ばれているはず。
        //   verify(モック, times(回数)).呼ばれたメソッド(引数) で「呼ばれた回数」を確かめられる。
        Mockito.verify(bedrockClient, Mockito.times(2)).invoke(Mockito.anyString());
    }

    // ==== ここから下は、あなたが書く問題 ============================================
    // 各問のコメントのヒントを読んで、@Disabled と fail(...) の2行を消して実装する。

    /**
     * 問3【易】レシピリンクが、3サイト分、料理名をURLエンコードして作られる。
     *
     * 見るところ: SuggestionService.buildRecipeLinks(...)
     *   (クックパッド・クラシル・楽天レシピの検索URLを組み立てているところ)
     */
    @Disabled("TODO: @Disabled と fail(...) の2行を消して実装する")
    @Test
    void 問3_レシピリンク_3サイト分が料理名をURLエンコードして作られる() {
        // ヒント1(準備): 問1と同じ。stubBedrock(bedrockResponse(FIVE_DISHES_JSON)) を呼ぶ。
        //
        // ヒント2(実行): 候補の1件目のリンク一覧を取り出す。
        //     var links = service.suggest(List.of("卵")).candidates().get(0).recipeLinks();
        //   1件目の料理名は「オムライス」。
        //
        // ヒント3(検証その1): サイト名の確認。
        //   links は RecipeLink(site, label, url) のリスト。site だけを取り出して順番ごと確認する。
        //     assertThat(links.stream().map(link -> link.site()))
        //             .containsExactly("cookpad", "kurashiru", "rakuten-recipe");
        //
        // ヒント4(検証その2): URLの確認。
        //   料理名「オムライス」は、URLの中では日本語のままではなくエンコードされた文字列になる。
        //   期待値は自分で encode("オムライス") を呼んで作ること(長い %E3... を手で書かない)。
        //     String encoded = encode("オムライス");
        //   期待するURLは3つ。実装(buildRecipeLinks)を見て、各サイトのURLの形を確認して書く。
        //     cookpad        : https://cookpad.com/jp/search/<encoded>
        //     kurashiru      : https://www.kurashiru.com/search?query=<encoded>
        //     rakuten-recipe : https://recipe.rakuten.co.jp/search/<encoded>/   ← 末尾に / がある点に注意
        //   links.stream().map(link -> link.url()) に containsExactly(...) をかければよい。
        //
        // つまずきやすい点: 文字列の連結は "https://.../" + encoded + "/" のように + で書く。
        fail("TODO: 問3を実装してください");
    }

    /**
     * 問4【易】AIがコードフェンス(```json ... ```)で囲んで返しても、パースできる。
     *
     * 背景: AIにはJSONだけを返すよう指示しているが、Claudeは ```json で囲んで返すことがある。
     *       以前これでパースに失敗するバグがあり、SuggestionService.stripCodeFence で対処した。
     *       つまりこれは「直したバグが再発しないこと」を守る回帰テスト。
     * 見るところ: SuggestionService.stripCodeFence(...)
     */
    @Disabled("TODO: @Disabled と fail(...) の2行を消して実装する")
    @Test
    void 問4_コードフェンス付きの応答でもパースできる() {
        // ヒント1(準備): AIの出力(text)を、コードフェンスで囲んだ文字列にする。
        //     String fenced = "```json\n" + FIVE_DISHES_JSON + "\n```";
        //   これを bedrockResponse(fenced) に渡し、stubBedrock(...) で返すよう指示する。
        //   (\n は改行。「```json」の次の行にJSONが来る形を再現している)
        //
        // ヒント2(実行): service.suggest(List.of("卵")) を呼ぶ。
        //
        // ヒント3(検証): 例外が出ずに、5件の候補が返ることを確認する。
        //     assertThat(result.candidates()).hasSize(5);
        //   フェンスの除去に失敗するとパース失敗 → リトライ → 例外になるので、
        //   「5件返った」だけでパースできた証明になる。
        //
        // 確認のコツ: SuggestionService の stripCodeFence の中身を一時的に壊して(例えば
        //   return trimmed; だけにして)このテストが赤くなるのを見ると、テストの意味が実感できる。
        //   確認したら元に戻すこと。
        fail("TODO: 問4を実装してください");
    }

    /**
     * 問5【易】JSONの前後に説明文がついていても、パースできる。
     *
     * 背景: 「以下が候補です。[ ...JSON... ] 以上です。」のように説明文付きで返ってくることもある。
     *       stripCodeFence は「最初の [ から最後の ] まで」を取り出すことで、これにも対応している。
     */
    @Disabled("TODO: @Disabled と fail(...) の2行を消して実装する")
    @Test
    void 問5_前置きの文章付きの応答でもパースできる() {
        // ヒント1(準備): 問4と同じ要領で、前後に文章を足した文字列を作る。
        //     String withText = "以下が候補です。\n" + FIVE_DISHES_JSON + "\n以上です。";
        //   bedrockResponse(withText) を stubBedrock(...) に渡す。
        //
        // ヒント2(実行・検証): suggest を呼び、5件返ることを確認する(問4と同じ形)。
        //
        // 発展: 文章の中に [ や ] を含む説明文(例: "候補[5件]です")をつけたらどうなる?
        //       実装を読んで、予想してから実際に試してみよう(この問の解答には含めなくてよい)。
        fail("TODO: 問5を実装してください");
    }

    /**
     * 問6【中】1回目が壊れたJSONでも、2回目で成功すれば結果を返す(リトライの確認)。
     *
     * 見るところ: SuggestionService.suggest(...) の
     *   tryFetchCandidates(prompt) を最大2回呼んでいる部分
     */
    @Disabled("TODO: @Disabled と fail(...) の2行を消して実装する")
    @Test
    void 問6_一回目が壊れたJSONでも二回目で成功すれば結果を返す() {
        // ヒント1(準備): stubBedrock は複数渡すと「順に」返す。1回目は壊れた応答、2回目は正常な応答にする。
        //     stubBedrock(
        //             bedrockResponse("これはJSONではありません"),   // 1回目
        //             bedrockResponse(FIVE_DISHES_JSON));            // 2回目
        //
        // ヒント2(実行): service.suggest(List.of("卵")) を呼ぶ。
        //   1回目で失敗しても、Serviceが自動でもう一度AIを呼ぶので、例外にはならない。
        //
        // ヒント3(検証その1): 結果が5件であること。
        //
        // ヒント4(検証その2): 「AIがちょうど2回呼ばれたこと」を確認する。
        //   これで「1回目で失敗 → リトライした」ことが証明できる。書き方は問2と同じ:
        //     Mockito.verify(bedrockClient, Mockito.times(2)).invoke(Mockito.anyString());
        //
        // つまずきやすい点: 2回呼ばれたことを確認しないと、
        //   「最初から2回とも成功していた」場合と区別がつかない。
        fail("TODO: 問6を実装してください");
    }

    /**
     * 問7【中】入力した食材が、AIに送るプロンプトに含まれる。
     *
     * 見るところ: SuggestionService.buildPrompt(...) と buildRequestBody(...)
     *   食材は「食材: 卵, 玉ねぎ。この食材で…」のようにプロンプトに埋め込まれ、
     *   さらに {"messages":[{"role":"user","content":"<プロンプト>"}], ...} というJSONで送られる。
     */
    @Disabled("TODO: @Disabled と fail(...) の2行を消して実装する")
    @Test
    void 問7_プロンプトに入力した食材が含まれる() throws Exception {
        // 狙い: モックに「どんな文字列が渡されたか」を覚えさせ、後から中身を調べる。
        //
        // ヒント1(準備): 渡された引数を保存する入れ物を用意する。
        //     AtomicReference<String> requestBody = new AtomicReference<>("");
        //   (ラムダ式の中から外の変数を書き換えるため、普通の String 変数ではなく AtomicReference を使う)
        //
        // ヒント2(準備): thenReturn ではなく thenAnswer を使うと、呼ばれた時の引数が見られる。
        //     Mockito.when(bedrockClient.invoke(Mockito.anyString())).thenAnswer(invocation -> {
        //         requestBody.set(invocation.getArgument(0));      // 0番目の引数 = 送られたJSON文字列
        //         return bedrockResponse(FIVE_DISHES_JSON);        // 戻り値は今まで通り正常な応答
        //     });
        //
        // ヒント3(実行): service.suggest(List.of("卵", "玉ねぎ")) を呼ぶ。
        //
        // ヒント4(検証): 保存したJSON文字列から、プロンプトの文章を取り出して確認する。
        //     String prompt = objectMapper.readTree(requestBody.get())
        //             .at("/messages/0/content").asText();
        //   at("/messages/0/content") は「messages の0番目の content」という意味のJSONの住所。
        //   取り出した prompt に「卵」と「玉ねぎ」が含まれることを確認する。
        //     assertThat(prompt).contains("卵").contains("玉ねぎ");
        //
        // 補足: Mockito には ArgumentCaptor という別の道具もあるが、
        //   「渡された引数を調べたい」だけなら thenAnswer の方が書き方が単純で読みやすい。
        fail("TODO: 問7を実装してください");
    }

    /**
     * 問8【考える】AIが5件未満(例: 3件)を返したら、今の実装はどう動く?
     *
     * これは「正解がひとつではない」問題。
     * 実装を読み、現状の動きを予想してからテストを書き、実行して予想が合っていたか確かめよう。
     * (件数を検証するコードは入っているか?)
     */
    @Disabled("TODO: @Disabled と fail(...) の2行を消して実装する")
    @Test
    void 問8_AIが5件未満を返したときの挙動() {
        // ヒント1(準備): 3件だけのJSONを自分で作る(必要な項目は name, description, calories, fat, sugar, carbs の6つ)。
        //     String threeDishes = """
        //             [
        //               {"name":"A","description":"a","calories":1,"fat":1,"sugar":1,"carbs":1},
        //               {"name":"B","description":"b","calories":2,"fat":2,"sugar":2,"carbs":2},
        //               {"name":"C","description":"c","calories":3,"fat":3,"sugar":3,"carbs":3}
        //             ]
        //             """;
        //   """ ... """ は複数行の文字列を書けるJavaの書き方(テキストブロック)。
        //
        // ヒント2(実行・検証): 実装を読むと、件数のチェックはどこにもない。
        //   予想: 3件のまま返るのか? エラーになるのか? それを assertThat(...) で書いてから実行する。
        //
        // ヒント3(考える): もし「必ず5件でなければエラーにしたい」仕様なら、
        //   どこを直せばいい? そのときこのテストはどう書き換わる?
        //   (今日はテストだけ書けば十分。実装の変更は余力があればやってみよう)
        fail("TODO: 問8を実装してください");
    }

    /**
     * 問9【考える】caloriesが数値ではなく文字列(例: "約450")で返ってきたら?
     *
     * 見るところ: dto/AiCandidate.java (calories は Integer)
     *   → 数値に変換できない文字列だと、JSONの読み込みが失敗する。
     *   → 失敗すると suggest はリトライし、それでもだめなら例外になる(問2と同じ流れ)。
     */
    @Disabled("TODO: @Disabled と fail(...) の2行を消して実装する")
    @Test
    void 問9_caloriesが数値でない応答は例外になる() {
        // ヒント1(準備): 1件だけで構わないので、calories が "約450"(文字列)のJSONを作る。
        //   Javaの文字列の中では " を \" と書く必要がある点に注意。
        //     String invalid = "[{\"name\":\"A\",\"description\":\"a\",\"calories\":\"約450\","
        //             + "\"fat\":1,\"sugar\":1,\"carbs\":1}]";
        //   これを bedrockResponse(invalid) に渡して stubBedrock(...) で毎回返す。
        //
        // ヒント2(実行・検証): 問2と同じ形で、AiResponseException が投げられることを確認する。
        //     assertThrows(AiResponseException.class, () -> service.suggest(List.of("卵")));
        //
        // ヒント3(検証): リトライするので、AIの呼び出しは2回。問2と同じ verify を書く。
        //
        // 発展: "450" のように、数字だけの文字列だとどうなる?
        //       (Jackson は "450" を 450 に自動変換してくれる。本当にそうなるか試してみよう)
        fail("TODO: 問9を実装してください");
    }

    /**
     * 問10【考える】AIの応答で、項目(例: name)が抜けていたら?
     *
     * 見るところ: SuggestionService.requireAllFields(...)
     *   Javaの record は、JSONに項目が無いと「エラー」ではなく null を入れてしまう。
     *   そこで、抜けを検出して「不正な応答」として扱う処理を足してある。
     */
    @Disabled("TODO: @Disabled と fail(...) の2行を消して実装する")
    @Test
    void 問10_項目が欠けた応答は例外になる() {
        // ヒント1(準備): name が無いJSONを作る(description 以降はある)。
        //     String missingName = "[{\"description\":\"a\",\"calories\":1,"
        //             + "\"fat\":1,\"sugar\":1,\"carbs\":1}]";
        //   bedrockResponse(missingName) を stubBedrock(...) に渡す。
        //
        // ヒント2(実行・検証): 問9と同じ形。AiResponseException が投げられ、AIが2回呼ばれることを確認する。
        //
        // 発展1: name 以外の項目(calories など)が欠けた場合も、同じテストを書いてみよう。
        // 発展2: requireAllFields の呼び出しを一時的にコメントアウトすると、このテストはどうなる?
        //        (確認したら元に戻すこと)
        fail("TODO: 問10を実装してください");
    }
}
