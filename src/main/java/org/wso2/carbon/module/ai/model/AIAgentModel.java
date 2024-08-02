package org.wso2.carbon.module.ai.model;

import org.wso2.carbon.module.ai.constants.AIConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class of any AI Agent.
 */
public abstract class AIAgentModel {
    private final AIEngineModel engine;
    private String basePrompt = "";

    private String retryPrompt = "";

    private final List<String> userMessages = new ArrayList<>();

    private int retryCount = AIConstants.RETRY_COUNT_DEFAULT;

    private int remainingRetries = AIConstants.RETRY_COUNT_DEFAULT;
    private String response = null;

    public AIAgentModel(AIEngineModel engine) {
        this.engine = engine;
    }

    public AIEngineModel getEngine() {
        return engine;
    }

    public String getBasePrompt() {
        return basePrompt;
    }

    public void setBasePrompt(String basePrompt) {
        this.basePrompt = basePrompt;
    }

    public String getRetryPrompt() {
        return retryPrompt;
    }

    public void setRetryPrompt(String retryPrompt) {
        this.retryPrompt = retryPrompt;
    }

    public List<String> getUserMessages() {
        return userMessages;
    }

    public void addUserMessage(String userMessage) {
        this.userMessages.add(userMessage);
    }

    public int getRetryCounter() {
        return remainingRetries;
    }

    public void setRetryCounter() {
        remainingRetries--;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        this.remainingRetries = retryCount;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    /**
     * Implementation should call the AI endpoint with necessary parameters and set the response
     */
    public abstract void processRequest();
}
