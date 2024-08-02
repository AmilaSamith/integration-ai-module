package org.wso2.carbon.module.ai.constants;

public class AIConstants {
    // AI Common Constants
    public static final String KEY_STRING = "openAIKey";
    public static final String MODEL_STRING = "openAIModel";
    public static final String MODEL_DEFAULT = "gpt-3.5-turbo";
    public static final String ENDPOINT_STRING = "openAIEndpoint";
    public static final String ENDPOINT_DEFAULT= "https://api.openai.com/v1/chat/completions";

    // AI Converser Constants
    public static final String PAYLOAD_STRING = "payload";
    public static final String RETRY_COUNT_STRING = "retryCount";
    public static final Integer RETRY_COUNT_DEFAULT = 3;
    public static final String PROMPT_STRING = "prompt";
    public static final String HEADERS_STRING = "headers";

    public static final String MESSAGE_TYPE_STRING = "messageType";
    public static final String CONTENT_TYPE_STRING = "ContentType";
    public static final String JSON_CONTENT_TYPE = "application/json";

    // AI Scanner Constants
    public static final String MAX_TOKENS = "maxTokens";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_CONTENT = "FileContent";

    public static final Integer MAX_TOKENS_DEFAULT = 500;
    public static final Integer IMAGE_DPI_DEFAULT = 300;
    public static final String SCANNER_OUTPUT_SCHEMA = "scannerOutputSchema";
    public static final String IMAGE_INPUT_TYPE_REGEX = "([\\S]*.(gif|jpe?g|tiff?|png|webp|bmp)$)";
    /**
     * Prompt String when schema is not provided
     */
    public static final String NO_SCHEMA_PROMPT_STRING = "Retrieve all form fields and values as JSON (get XML " +
            "containing fields also not xml tags whole one as one field with actual field name in front it), return as " +
            "JSON object {fieldName:FieldValue} including all fields . The key names should match the field names " +
            "exactly and should be case sensitive. Scan the whole document and retrieve all fields.";

    /**
     * Prompt String when schema is provided
     */
    public static final String SCHEMA_PROMPT_STRING_1 = "Retrieve all form fields and  values as json (get XML " +
            "containing fields also not xml tags whole one as one field with actual field name in front it), return " +
            "as provided schema (Only values related to schema) ,";
    public static final String SCHEMA_PROMPT_STRING_2 = " as json object {fieldName:FieldValue} only fields in " +
            "schema, only return this object is enough, these are should be keys of the json under content, key " +
            "names should be same as provided, case sensitive (scan whole document and check, get all fields there " +
            "could be some lengthy fields,xml fields like value),Please return null if there is no matching attribute , schema to image.";
}

