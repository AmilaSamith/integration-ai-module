package org.wso2.carbon.module.ai.model.prompt;


import com.azure.ai.openai.models.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.wso2.carbon.module.ai.model.AIAgentModel;
import org.wso2.carbon.module.ai.model.AIEngineModel;
import org.wso2.carbon.module.ai.util.AIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Agent to run a given prompt.
 */
public class AIConverserAgentModel extends AIAgentModel {

    private final AIRequestSchema requestSchema;

    private final AIResponseSchema responseSchema = new AIResponseSchema();;

    public AIConverserAgentModel(AIEngineModel engine, AIRequestSchema requestSchema) {
        super(engine);
        this.requestSchema = requestSchema;
        this.setBasePrompt("You are a helpful assistant who do what I tell you. I will provide a request-schema and a " +
                "response-schema. The task to do is available in the request-schema (prompt prop). The data you have " +
                "to process is available in the request-schema (payload prop). Avoid xmlns attribute if data is " +
                "given in xml format. When you're giving an answer, always use the response-schema. The processed " +
                "data should be in the data prop. Only add processed data under data prop. " +
                "This processed data can be json, xml  or text. It depends on the " +
                "task. Set the dataType prop in response to 'json', 'xml' or 'text' depending on the processed " +
                "data. And don't forget to set the status prop to 'success' or 'error' depending on the result of " +
                "the task.");

        this.setRetryPrompt("The response you provided is not in the given format in response-schema. Please provide a " +
                "valid response.");

        Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        this.addUserMessage("request-schema:" + gsonBuilder.toJson(this.requestSchema)
                + " response-schema:" + gsonBuilder.toJson(responseSchema));
    }

    public AIRequestSchema getRequestSchema() {
        return requestSchema;
    }

    public AIResponseSchema getResponseSchema() {
        return responseSchema;
    }

    @Override
    public void processRequest() {
        AIEngineModel engine = getEngine();
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatRequestSystemMessage(getBasePrompt()));
        getUserMessages().forEach(message -> {
            chatMessages.add(new ChatRequestUserMessage(message));
        });

        boolean noValidResponse = true;
        String response = "";
        while (noValidResponse && getRetryCounter() > 0) {
            ChatCompletions chatCompletions = engine.getOpenAIClient().getChatCompletions(
                    engine.getOpenaiModel(),
                    new ChatCompletionsOptions(chatMessages)
            );

            for (ChatChoice chat : chatCompletions.getChoices()) {
                response = chat.getMessage().getContent();
                if (!AIUtils.isInvalidResponse(response)) {
                    noValidResponse = false;
                    break;
                }
            }
            if (noValidResponse) {
                chatMessages.add(new ChatRequestAssistantMessage(response));
                chatMessages.add(new ChatRequestUserMessage(getRetryPrompt()));
                setRetryCounter();
            }
        }

        if (noValidResponse) {
            setResponse("Failed to get a valid JSON response after " + getRetryCount() + " attempts.");
        } else {
            setResponse(response);
        }
    }
}
