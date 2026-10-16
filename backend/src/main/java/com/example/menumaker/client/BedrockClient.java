package com.example.menumaker.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * AWS Bedrock (Claude) の呼び出しをラップするクライアント。
 * 他のクラスからはAWS SDKへの依存を隠す。
 */
@Component
public class BedrockClient {

    private final String region;
    private final String modelId;

    // 認証情報が無くてもアプリが起動できるよう、SDKクライアントは初回利用時に生成する。
    private volatile BedrockRuntimeClient client;

    public BedrockClient(
            @Value("${aws.region}") String region,
            @Value("${aws.bedrock.model-id}") String modelId) {
        this.region = region;
        this.modelId = modelId;
    }

    private BedrockRuntimeClient client() {
        BedrockRuntimeClient result = client;
        if (result == null) {
            synchronized (this) {
                result = client;
                if (result == null) {
                    result = BedrockRuntimeClient.builder()
                            .region(Region.of(region))
                            .build();
                    client = result;
                }
            }
        }
        return result;
    }

    /**
     * プロンプトを渡してBedrockを呼び出し、レスポンスのJSON文字列全体を返す。
     */
    public String invoke(String requestBodyJson) {
        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestBodyJson))
                .build();

        InvokeModelResponse response = client().invokeModel(request);
        return response.body().asUtf8String();
    }
}
