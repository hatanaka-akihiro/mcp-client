# 🧩 MCP Java Client Starter (Maven Project)

このプロジェクトは、**Model Context Protocol (MCP)** クライアントを Java で構築するための Maven ベースのスタータープロジェクトです。  
MCP Server に接続し、ツール・リソース・プロンプトの一覧取得やツール呼び出し (`callTool`) を行うことができます。

---

## 🚀 概要

**Model Context Protocol (MCP)** は、AI（LLM）やエージェントが外部システムと安全かつ統一的に通信するためのオープンプロトコルです。  
このプロジェクトでは、公式の **MCP Java SDK** を利用し、以下を実装します：

- MCP Server への接続と初期化 (`initialize`)
- 利用可能な機能の発見 (`tools/list`, `resources/list`, `prompts/list`)
- ツール呼び出し (`callTool`)
- 結果のハンドリングと出力

---

## 📁 プロジェクト構成

```
mcp-java-client/
├── src/
│   ├── main/java/com/example/mcp/
│   │   ├── App.java              # 実行エントリーポイント
│   │   └── McpClientRunner.java  # MCPクライアントの初期化・操作
│   └── test/java/com/example/mcp/
│       └── AppTest.java          # サンプルテスト
├── pom.xml                       # Maven設定ファイル
└── README.md                     # このファイル
```

---

## ⚙️ 環境要件

| 要件 | バージョン |
|------|-------------|
| Java | 17 以上 |
| Maven | 3.6 以上 |
| MCP Server | HTTP/SSE/STDIO に対応したサーバ |

---

## 📦 セットアップ手順

### 1. プロジェクト作成
```bash
mvn archetype:generate \
  -DgroupId=com.example.mcp \
  -DartifactId=mcp-java-client \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
cd mcp-java-client
```

### 2. `pom.xml` に依存関係を追加

```xml
<dependencies>
  <!-- MCP Java SDK -->
  <dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>1.0.0</version>
  </dependency>

  <!-- JSON処理 -->
  <dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.0</version>
  </dependency>

  <!-- ロギング -->
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.9</version>
  </dependency>

  <!-- テスト -->
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

---

## 🧠 サンプルコード

### `src/main/java/com/example/mcp/App.java`

```java
package com.example.mcp;

public class App {
    public static void main(String[] args) throws Exception {
        McpClientRunner.run();
    }
}
```

### `src/main/java/com/example/mcp/McpClientRunner.java`

```java
package com.example.mcp;

import io.modelcontextprotocol.sdk.client.*;
import io.modelcontextprotocol.sdk.transport.http.*;
import java.time.Duration;
import java.util.Map;

public class McpClientRunner {

    public static void run() throws Exception {
        System.out.println("=== MCP Java Client Example ===");

        // 1. トランスポート設定（HTTP）
        var transport = McpTransportBuilder
            .forHttp("http://localhost:8000/mcp")
            .timeout(Duration.ofSeconds(30))
            .build();

        // 2. クライアント作成
        var client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .build();

        // 3. 初期化
        client.initialize();

        // 4. ツール一覧取得
        var tools = client.tools().list();
        System.out.println("== Available Tools ==");
        tools.forEach(t -> System.out.println("- " + t.name() + " : " + t.description()));

        // 5. ツール呼び出し例
        var result = client.callTool(
            new CallToolRequest("createTicket", Map.of(
                "title", "テスト課題",
                "body", "MCPクライアントから作成された課題",
                "priority", "high"
            ))
        );

        System.out.println("== Call Result ==");
        System.out.println(result.result());

        client.closeGracefully();
    }
}
```

---

## 🧪 実行手順

### ビルド
```bash
mvn clean package
```

### 実行
```bash
java -jar target/mcp-java-client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## 🧰 テスト用 MCP Server（ローカル実行例）

Node.js があれば、以下の MCP サーバを起動して接続確認できます：

```bash
npx -y @modelcontextprotocol/server-everything dir
```

このサーバは `http://localhost:8000/mcp` で MCP エンドポイントを提供します。  
上記 URL を `McpClientRunner` の接続先として設定してください。

---

## 🪶 今後の拡張アイデア

- [ ] `resources/list` や `prompts/list` の対応を追加
- [ ] 接続エラー時の再試行とロギングの強化
- [ ] Questetra BPM Suite から HTTP 経由で呼び出すブリッジ化
- [ ] AWS Bedrock AgentCore との連携デモ
- [ ] Spring AI MCP モジュールとの統合

---

## 📚 参考リンク

- 🌐 公式サイト: [https://modelcontextprotocol.io](https://modelcontextprotocol.io)
- ☕ Java SDK: [https://github.com/modelcontextprotocol/java-sdk](https://github.com/modelcontextprotocol/java-sdk)
- 🧩 Spring AI MCP: [https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)

---

## 🧾 ライセンス

MIT License © 2025
