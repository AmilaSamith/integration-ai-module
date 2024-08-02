package org.wso2.carbon.module.ai.model.prompt;

/**
 * A template class for sending the request to OpenAI in prompt mode.
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class AIRequestSchema {
    private final String prompt;
    private final String payload;
    private final String headers;

    public AIRequestSchema(String prompt, String payload, String headers) {
        this.prompt = prompt;
        this.payload = payload;
        this.headers = headers;
    }
}