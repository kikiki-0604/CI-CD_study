# 困ったとき(よくあるエラーと対処)

まず**エラーメッセージを、そのまま読む**ことから始めましょう。多くの場合、原因がそこに書いてあります。
それでも解決しないときは、**エラーメッセージの全文**を持って、進行役に声をかけてください。

## 手元の PC

| 症状 | 原因 | 対処 |
| --- | --- | --- |
| `Cannot connect to the Docker daemon` | Docker Desktop が起動していない | Docker Desktop を起動して、クジラのアイコンが落ち着くまで待つ |
| `port is already allocated`(5173、8080、3306) | 他のアプリが同じポートを使っている | 他のアプリを終了する。前回の `docker compose` が残っていれば `docker compose down` |
| テストの実行がとても遅い | 初回は、ライブラリのダウンロードがある | 数分待つ。2回目以降は、`gradle-cache` のおかげで速くなる |
| テスト実行で `no matching tests found` | `--tests` の指定が、テスト名に合っていない | `"*SuggestionServiceTest.問3_*"` のように、**問の番号のあとの `_`** まで書く |
| `問1` を指定したのに `問10` も動いた | `_` が抜けている | `問1_*` と書く |
| 画面で「候補の取得に失敗しました」 | Bedrock の API キーが間違っている、または切れている。または、短時間に呼びすぎた | `backend/.env` のキーを見直す。数十秒待ってから再試行する。`docker compose logs backend` で原因を見る |
| `.env` を変えたのに反映されない | コンテナが古い設定のまま | `docker compose up --build`(または `docker compose restart backend`) |
| `docker compose` で日本語の履歴が文字化けする | 初回のデータ作りに失敗している | `docker compose down -v` → `docker compose up --build` で DB を作り直す |

## Git / GitHub

| 症状 | 原因 | 対処 |
| --- | --- | --- |
| `git push` で認証に失敗する | GitHub へのログインができていない | ブラウザの認証画面の案内に従う。うまくいかなければ進行役へ |
| `git push` が `rejected`(non-fast-forward) | 他の人が先に push した | `git pull origin main` で取り込んでから、もう一度 push |
| `CONFLICT`(マージのコンフリクト) | 同じ場所を、複数人が編集した | ファイルを開くと `<<<<<<<` と `>>>>>>>` で、両方の変更が挟まれている。**どちらを残すか決めて**、印を消して、`git add` → `git commit` |
| PR の画面に「Merge」ボタンが出ない / 押せない | CI がまだ動いている、または権限がない | CI の完了を待つ。権限は、リポジトリの代表に確認する |
| 間違えて `.env` を `git add` してしまった | — | まだ commit していなければ `git reset backend/.env`。**commit してしまったら、すぐ進行役へ**(キーを無効にして作り直します) |

## CI(GitHub Actions)

まず、**Actions タブ → 失敗した実行 → 赤い ✗ のジョブ → 赤くなったステップ**を開き、ログの**最後の方**を読みます。

| 症状 | 原因 | 対処 |
| --- | --- | --- |
| `backend` が赤く、`FAILED` が出ている | テストが失敗している | ログで、どのテスト(`問N_...`)が、**期待値と何が違った**かを読む。手元で、その問だけ実行して直す |
| `backend` が赤く、`error: cannot find symbol` など | コンパイルエラー(文字の打ち間違い、import の漏れ) | エラーの**ファイル名と行番号**を見る。手元でも、同じエラーが再現できる |
| `frontend` が赤く、`TS...` というエラー | TypeScript の型エラー | エラーの行を見て直す |
| `docker images` が赤く、`NoCredentialsError` や `could not be loaded` | AWS のキーが登録されていない、または名前が違う | **ステップ4**を見直す。名前が `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` と完全に一致しているか(大文字小文字、前後の空白) |
| `docker images` が赤く、`The security token included in the request is invalid` / `SignatureDoesNotMatch` | キーの値が間違っている(コピーミス、改行の混入) | Secrets を**登録し直す**(上書きできる)。値の前後に空白や改行が入っていないか確認 |
| `docker images` が赤く、`denied` / `AccessDenied`(ECR) | チームのキーで、他チームの置き場に入れようとした | キーが、自分たちのチームのものか確認。進行役へ |
| `deploy to EC2` が赤く、`EC2 (Owner=...) が見つかりません` | チームのサーバーが見つからない | 進行役へ(サーバーが停止している可能性) |
| `deploy to EC2` が赤く、`deploy failed: Failed` | サーバー上での入れ替えが失敗した | ログ(ステップ `Deploy via SSM` の出力)の**最後の方**を、進行役に見せる |
| `Health check` が赤く、`health check failed` | デプロイは済んだが、アプリが応答しない(起動に時間がかかっている、またはアプリが落ちている) | **しばらく待って、Actions の「Re-run failed jobs」**を試す。それでもだめなら、進行役へ |
| 緑なのに `deploy` が実行されていない(スキップ表示) | PR からの実行、または AWS のキー未登録 | `main` への変更のときだけ、デプロイされる。キー未登録なら、実行結果に黄色い警告が出る |
| テストが緑なのに、8問が「スキップ」になっている | `@Disabled` が残っている | その問の `@Disabled` と `fail(...)` の**2行を消す**のを忘れていないか確認 |

## デプロイ先のアプリ

| 症状 | 原因 | 対処 |
| --- | --- | --- |
| URL を開いても、つながらない | まだデプロイされていない、または、起動中 | Actions で `deploy` が緑か確認。緑なら、数十秒待ってリロード |
| 画面は出るが、候補が出ない | AI の呼び出しに失敗している(上限に達した、など) | 少し待って、もう一度試す。続く場合は進行役へ |
| 変更したのに、画面が前のまま | ブラウザのキャッシュ、または、デプロイが終わっていない | `Ctrl + Shift + R`(強制リロード)。Actions の完了を確認 |

## 進行役に伝えるとき、あると助かる情報

- 何をしようとしていたか(どのステップか)
- エラーメッセージの**全文**(スクリーンショット可)
- Actions の失敗したジョブの、**赤くなったステップのログ**
- **キーそのものは、絶対に見せないでください**(Secrets の値、`.env` の中身)
