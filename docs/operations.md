# 実行手順書(手元での起動・テスト)

手元の PC でアプリを動かし、テストを実行するための手順書です。
勉強会全体の流れは [勉強会の進め方](workshop-guide.md) を、クラウドへのデプロイの仕組みは [CI/CD の解説](cicd-overview.md) を参照してください。

## 事前準備

- [ ] Docker Desktop がインストールされ、起動している(タスクバーにクジラのアイコンが表示されている状態)
- [ ] Git が使える(`git --version` でバージョンが表示される)
- [ ] 配布された **Bedrock の API キー**を受け取っている

```bash
docker --version
docker compose version
```

どちらもバージョンが表示されれば OK です。エラーになる場合は Docker Desktop が起動していません。

Java や Node.js を個別にインストールする必要はありません(実行環境はコンテナの中にあります)。

## 初回セットアップ

### 1. 環境変数ファイルを作る

`backend/.env.example` をコピーして `backend/.env` を作り、配布された Bedrock の API キーを書きます。

```bash
cp backend/.env.example backend/.env
```
(Windows PowerShell では `copy backend\.env.example backend\.env`)

`backend/.env` を開き、次の行を埋めます。

```
AWS_BEARER_TOKEN_BEDROCK=配布されたキー
AWS_REGION=ap-northeast-1
BEDROCK_MODEL_ID=jp.anthropic.claude-haiku-4-5-20251001-v1:0
```

> **注意**: `.env` は絶対に GitHub に push しないでください(`.gitignore` で除外済みですが、`git status` で `.env` が候補に出ていないか、確認すると安心です)。

### 2. 起動する

リポジトリのルート(`docker-compose.yml` がある場所)で実行します。

```bash
docker compose up --build
```

初回は数分かかります。ログが流れ続ける状態になります(止めるときは `Ctrl + C`)。
バックグラウンドで動かしたい場合は `docker compose up --build -d` です。

### 3. 動作を確認する

| 確認対象 | 方法 | 期待する結果 |
| --- | --- | --- |
| フロントエンド | ブラウザで http://localhost:5173 を開く | 食材入力画面が表示される |
| バックエンド | `curl http://localhost:8080/api/records` | 初期データ(肉じゃが、オムライスなど6件)が JSON で返る |
| 全体 | 画面で食材を入力して送信 | 料理の候補が5件表示される(AI を呼ぶので数秒かかる) |

## テストの実行

Java のインストールは不要です。Docker 上の Gradle で実行します。**リポジトリのルート**で実行してください。

**Mac / Linux / Git Bash**

```bash
docker run --rm -v "$(pwd)/backend:/app" -v gradle-cache:/home/gradle/.gradle -w /app gradle:8.9-jdk17 gradle test --no-daemon --console=plain
```

**Windows PowerShell**

```powershell
docker run --rm -v "${PWD}/backend:/app" -v gradle-cache:/home/gradle/.gradle -w /app gradle:8.9-jdk17 gradle test --no-daemon --console=plain
```

- 初回は、ライブラリのダウンロードで数分かかります。`gradle-cache` という Docker のボリュームに保存されるため、2回目以降は速くなります。
- **1つのテストだけ**実行したいときは、末尾に `--tests "*SuggestionServiceTest.問3_*"` を付けます。**問の番号のあとの `_` を忘れないでください**。

## 日常の操作

### ログを見る

```bash
docker compose logs -f backend   # backendだけ
docker compose logs -f           # 全部
```

### コードを直したら

この構成は、ソースコードのボリュームマウント(ホットリロード)を行いません。**コードを保存しただけでは反映されません**。再ビルドしてください。

```bash
docker compose up --build backend    # backend だけ再ビルド
docker compose up --build frontend   # frontend だけ再ビルド
docker compose up --build            # まとめて再ビルド
```

`docker compose restart <サービス名>` は再ビルドを行わないため、コードの変更には効きません。

### 停止する

```bash
docker compose down
```

DB のデータも消して、完全に初期化したいときは `-v` を付けます(記録が全部消えます)。

```bash
docker compose down -v
```

## コマンドチートシート

| コマンド | 説明 |
| --- | --- |
| `docker compose up --build` | 再ビルドして起動する |
| `docker compose up -d` | バックグラウンドで起動する |
| `docker compose ps` | 各コンテナの状態を見る |
| `docker compose logs -f [サービス名]` | ログを見る |
| `docker compose down` | 停止する(データは残る) |
| `docker compose down -v` | 停止し、DB のデータも削除する |

うまくいかないときは、[困ったとき](troubleshooting.md) を見てください。
