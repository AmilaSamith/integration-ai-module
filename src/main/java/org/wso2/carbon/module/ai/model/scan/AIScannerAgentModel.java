package org.wso2.carbon.module.ai.model.scan;

import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.synapse.SynapseException;
import org.wso2.carbon.module.ai.constants.AIConstants;
import org.wso2.carbon.module.ai.model.AIAgentModel;
import org.wso2.carbon.module.ai.model.AIEngineModel;
import org.wso2.carbon.module.ai.util.AIUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wso2.carbon.module.ai.util.AIUtils.loadJsonData;

/**
 * AI Agent to scan documents.
 */
public class AIScannerAgentModel extends AIAgentModel {

    private Integer maxTokens = AIConstants.MAX_TOKENS_DEFAULT;
    private String fileName = "";
    private String fileContent = "";
    private String schemaRegistryPath = "";

    public AIScannerAgentModel(AIEngineModel engine) {
        super(engine);
        this.setBasePrompt("You are an intelligent assistant tasked with analyzing text extracted from " +
                "images using OCR technology. Your goal is to understand the content and provide insights based on the " +
                "extracted text.");
    }

    @Override
    public void processRequest() {
        // Constructing the JSON payload
        String payload = null;
        try {
            payload = generateScanRequestPayload();
        } catch (SynapseException e) {
            throw new SynapseException("Error occurred in generating OpenAI image Scan Request Payload", e);
        }

        // Get connection
        HttpURLConnection connection = AIUtils.getConnection(this);

        // Sending the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (IOException e) {
            throw new SynapseException("Request time out", e);
        }

        // Reading the response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            // Parse the JSON response string into a JsonObject
            JsonObject jsonObject = loadJsonData(response.toString());

            // Get the content string
            assert jsonObject != null;
            String contentString = jsonObject.getAsJsonArray("choices").get(0).getAsJsonObject().
                    getAsJsonObject("message").get("content").getAsString();
            setResponse(contentString);

        } catch (IOException e) {
            throw new SynapseException("Error occurred in fetching OpenAI response : " + e.getMessage());
        }

        // Disconnecting the connection
        connection.disconnect();
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getSchemaRegistryPath() {
        return schemaRegistryPath;
    }

    public void setSchemaRegistryPath(String schemaRegistryPath) {
        this.schemaRegistryPath = schemaRegistryPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    /**
     * Generate total prompt message with images to send to ChatGPT
     *
     * @return payload as String
     */
    private String generateScanRequestPayload() throws SynapseException {
        //Hold total image url messages as a String
        StringBuilder imageRequestPayload;

        // Reading the schema file
        InputStream schemaStream = AIUtils.getSchemaFromRegistry(getSchemaRegistryPath());
        String schema = "";
        if (schemaStream != null) {
            try {
                schema = IOUtils.toString(schemaStream, String.valueOf(StandardCharsets.UTF_8))
                        .replace("\"", "")
                        .replace("\t", "").replace("\n", "");
            } catch (IOException e) {
                throw new SynapseException("Error with the output schema content reading", e);
            }
        } else {
            schema = "";
        }

        // Perform document type check
        if (fileName.toLowerCase().endsWith("pdf")) {
            List<String> base64_images = AIUtils.pdfToImage(fileContent);
            List<String> imageRequestList = new ArrayList<>();

            for (int i = 0; i < Objects.requireNonNull(base64_images).size(); i++) {
                imageRequestList.add(AIUtils.getImageMessegeString(base64_images.get(i)));
            }

            imageRequestPayload = new StringBuilder(String.join(",", imageRequestList));
        } else {
            Pattern regexPattern = Pattern.compile(AIConstants.IMAGE_INPUT_TYPE_REGEX);
            Matcher matcher = regexPattern.matcher(fileName.toLowerCase());

            if (matcher.matches()) {
                imageRequestPayload = new StringBuilder(AIUtils.getImageMessegeString(fileContent));
            } else {
                throw new SynapseException("Invalid file format with the payload");
            }
        }

        // return the JSON payload
        return AIUtils.getOpenAIMessagePayload(
                this.getEngine().getOpenaiModel(), imageRequestPayload, schema, this.maxTokens);
    }

}
