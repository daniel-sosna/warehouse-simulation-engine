package lt.bananull.whse.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JacksonMapper {
    private JacksonMapper() {}

    public static ObjectMapper create() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());

        // ISO-8601 strings for java.time when writing JSON
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Strict or tolerant:
        om.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return om;
    }
}