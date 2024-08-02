package org.wso2.carbon.module.ai.model;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;

/**
 * AI Engine Model class.
 */
public class AIEngineModel {

    private final String openaiKey;
    private final String openaiModel;
    private final String openaiEndpoint;

    public AIEngineModel(String openaiKey, String openaiModel, String openaiEndpoint) {
        this.openaiKey = openaiKey;
        this.openaiModel = openaiModel;
        this.openaiEndpoint = openaiEndpoint;
    }

    /**
     * Get a OpenAI Client object with Azure utilities.
     */
    public OpenAIClient getOpenAIClient(){
        OpenAIClientBuilder builder = new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(this.openaiKey))
                .endpoint(this.openaiEndpoint);

        return builder.buildClient();
    }

    public String getOpenaiModel() {
        return openaiModel;
    }

    public String getOpenaiKey() {
        return openaiKey;
    }

    public String getOpenaiEndpoint() {
        return openaiEndpoint;
    }
}
