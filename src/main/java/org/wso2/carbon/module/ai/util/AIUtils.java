package org.wso2.carbon.module.ai.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.wso2.carbon.connector.core.util.ConnectorUtils;
import org.wso2.carbon.module.ai.constants.AIConstants;
import org.wso2.carbon.module.ai.model.AIAgentModel;
import org.wso2.carbon.module.ai.model.AIEngineModel;
import org.wso2.carbon.module.ai.model.prompt.AIConverserAgentModel;
import org.wso2.carbon.module.ai.model.prompt.AIRequestSchema;
import org.wso2.carbon.module.ai.model.scan.AIScannerAgentModel;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;
import org.wso2.micro.integrator.registry.Resource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for AI operations.
 */
public class AIUtils {
    /**
     * This method will convert a given string into JSON object.
     *
     * @param json The JSON data in string format.
     * @return The JSON object.
     */
    public static JsonObject loadJsonData(String json) {
        try {
            return new JsonParser().parse(json).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    /**
     * Reading the schema from the registry
     *
     * @param schemaPath     Registry Path of the Schema
     * @return InputStream
     */
    public static InputStream getSchemaFromRegistry(String schemaPath) throws SynapseException{
        MicroIntegratorRegistry registry = new MicroIntegratorRegistry();
        Resource resource;
        InputStream schemaStream;
        if (!schemaPath.isEmpty()) {
            resource = registry.getResource(schemaPath);
            try {
                schemaStream = resource.getContentStream();
                return schemaStream;
            } catch (IOException e) {
                throw new SynapseException("Error while reading schema from registry", e);
            }
        }
        return null;
    }

    /**
     * Convert PDF to Images and return as Base64 String List
     *
     * @param base64Pdf - Content of the pdf in Base64
     * @return List of images
     */
    public static List<String> pdfToImage(String base64Pdf) {
        try (InputStream pdfInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64Pdf))) {
            PDDocument document = PDDocument.load(pdfInputStream);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            List<BufferedImage> images = new ArrayList<>();
            List<String> encodedImages = new ArrayList<>();

            int numberOfPages = document.getNumberOfPages();

            for (int i = 0; i < numberOfPages; ++i) {
                BufferedImage bImage = pdfRenderer.renderImageWithDPI(i, AIConstants.IMAGE_DPI_DEFAULT, ImageType.RGB);
                images.add(bImage);
            }

            document.close();

            for (BufferedImage image : images) {
                String base64Image = encodeConvertedImage(image);
                encodedImages.add(base64Image);
            }
            return encodedImages;
        } catch (Exception e) {
            throw new SynapseException("Error while converting pdf to image , incorrect Base64", e);
        }
    }

    /**
     * Get HTTP Connection to OpenAI API
     *
     * @param agent AIAgent
     * @return HttpURLConnection
     */
    public static HttpURLConnection getConnection(AIAgentModel agent) {
        //URL instance for connection
        URL url = null;
        try {
            url = new URL(agent.getEngine().getOpenaiEndpoint());
        } catch (MalformedURLException e) {
            throw new SynapseException("GPT endpoint url is not valid url", e);
        }

        //Constructing HttpURLConnection
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
        } catch (IOException e) {
            throw new RuntimeException(e + "Connection to the url unsuccessful");
        }

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + agent.getEngine().getOpenaiKey());
        connection.setDoOutput(true);

        return connection;
    }

    /**
     * Get image message string in OpenAI format
     *
     * @param image base64 string
     * @return Images message string
     */
    public static String getImageMessegeString(String image) {
        return "{\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64," +
                image + "\"}}";
    }

    /**
     * Get image message string in OpenAI format
     *
     * @param image base64 string
     * @return Images message string
     */
    public static String getOpenAIMessagePayload(String OpenAIModel, StringBuilder image, String schema, int maxTokens) {
        StringBuilder payloadBuilder = new StringBuilder();

        // Append common parts of the payload
        payloadBuilder.append("{\"model\": \"")
                .append(OpenAIModel)
                .append("\", \"messages\": [{\"role\": \"user\", \"content\": [{\"type\": \"text\", \"text\": \" ");

        // Append schema-related text based on whether schema exists
        if (!schema.isEmpty()) {
            payloadBuilder.append(AIConstants.SCHEMA_PROMPT_STRING_1)
                    .append(schema)
                    .append(AIConstants.SCHEMA_PROMPT_STRING_2);
        } else {
            payloadBuilder.append(AIConstants.NO_SCHEMA_PROMPT_STRING);
        }

        // Append image request payload and complete the JSON structure
        payloadBuilder.append("\"},")
                .append(image)
                .append("]}], \"response_format\": {\"type\": \"json_object\"}, \"max_tokens\":")
                .append(maxTokens)
                .append("}");

        // Convert StringBuilder to String and return
        return payloadBuilder.toString();
    }

    /**
     * Receive converted pdf pages as Buffered Images and return In Base64
     *
     * @param image Buffered Image
     * @return Images as Base64
     * @throws IOException
     */
    private static String encodeConvertedImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * This method will check whether the given response is in the correct format as per the response-schema.
     *
     * @param response The response from OpenAI.
     * @return Whether the response is in the correct format. (true | false)
     */
    public static boolean isInvalidResponse(String response) {
        JsonObject jsonNode = loadJsonData(response);

        if (jsonNode == null || jsonNode.get("data") == null) {
            return true;
        }

        if (jsonNode.get("dataType") != null) {
            String type = jsonNode.get("dataType").getAsString();
            switch (type.toLowerCase()) {
                case "json":
                case "xml":
                case "text":
                    return false;
                default:
                    return true;
            }
        }

        return true;
    }

    public static AIEngineModel getAIEngine(MessageContext mc) throws SynapseException {
        Optional<String> openAIKey = getStringParam(mc, AIConstants.KEY_STRING);
        String openAIModel = getStringParam(mc, AIConstants.MODEL_STRING).orElse(AIConstants.MODEL_DEFAULT);
        String openAIEndpoint = getStringParam(mc, AIConstants.ENDPOINT_STRING).orElse(AIConstants.ENDPOINT_DEFAULT);

        if (!openAIKey.isPresent()) {
            throw new SynapseException("OpenAI API key is required.");
        }

        return new AIEngineModel(openAIKey.get(), openAIModel, openAIEndpoint);
    }

    public static AIConverserAgentModel getAIConverserAgent(MessageContext mc) throws SynapseException {
        Optional<String> prompt = getStringParam(mc, AIConstants.PROMPT_STRING);
        Optional<String> payload = getStringParam(mc, AIConstants.PAYLOAD_STRING);
        String headers = getStringParam(mc, AIConstants.HEADERS_STRING).orElse("");
        Integer retryCount = getIntegerParam(mc, AIConstants.RETRY_COUNT_STRING).orElse(AIConstants.RETRY_COUNT_DEFAULT);

        AIRequestSchema requestSchema = new AIRequestSchema(
                prompt.orElseThrow(() -> new SynapseException("Prompt is required.")),
                payload.orElseThrow(() -> new SynapseException("Payload is required.")),
                headers
        );

        AIConverserAgentModel agent = new AIConverserAgentModel(getAIEngine(mc), requestSchema);
        agent.setRetryCount(retryCount);

        return agent;
    }

    public static AIScannerAgentModel getAIScannerAgent(MessageContext mc) throws SynapseException {
        AIScannerAgentModel agent = new AIScannerAgentModel(getAIEngine(mc));

        Integer maxTokens = getIntegerParam(mc, AIConstants.MAX_TOKENS).orElse(AIConstants.MAX_TOKENS_DEFAULT);
        String schemaFile = getStringParam(mc, AIConstants.SCANNER_OUTPUT_SCHEMA).orElse("");
        String fileName = getStringParam(mc, AIConstants.FILE_NAME).orElse("");
        String fileContent = getStringParam(mc, AIConstants.FILE_CONTENT).orElse("");

        if (fileName.isEmpty()) {
            throw new SynapseException("Cannot find the filename to process");
        } else if (fileContent.isEmpty()) {
            throw new SynapseException("Cannot find the content to process");
        }

        agent.setFileName(fileName);
        agent.setFileContent(fileContent);

        if (!(maxTokens >0)) {
            throw new SynapseException("Invalid number of tokens.");
        }

        if (schemaFile.trim().endsWith(".xsd") || schemaFile.trim().
                endsWith(".json") || schemaFile.isEmpty()) {
            agent.setSchemaRegistryPath(schemaFile);
        } else {
            throw new SynapseException("Invalid file type, type should be xsd or json");
        }

        agent.setMaxTokens(maxTokens);
        agent.setSchemaRegistryPath(schemaFile);

        return agent;
    }

    /**
     * Read a String parameter
     * @param mc MessageContext.
     * @param parameterKey Key of the parameter.
     * @return Optional String of the parameter value.
     */
    public static Optional<String> getStringParam(MessageContext mc, String parameterKey) {
        String parameter = (String) ConnectorUtils.lookupTemplateParamater(mc, parameterKey);
        if (StringUtils.isNotBlank(parameter)) {
            return Optional.of(parameter);
        }
        return Optional.empty();
    }

    /**
     * Read a Integer parameter
     * @param mc MessageContext.
     * @param parameterKey Key of the parameter.
     * @return Optional String of the parameter value.
     */
    public static Optional<Integer> getIntegerParam(MessageContext mc, String parameterKey) {
        Optional<String> parameterValue = getStringParam(mc, parameterKey);
        return parameterValue.map(s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

}
