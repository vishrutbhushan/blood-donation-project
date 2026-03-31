package etl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unchecked")
public class JsonUtil {
    private final ObjectMapper objectMapper;

    public JsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> readFileMap(String path) {
        try {
            Path p = Path.of(path);
            if (!Files.exists(p)) {
                return new LinkedHashMap<>();
            }
            Object value = objectMapper.readValue(p.toFile(), Object.class);
            if (!(value instanceof Map)) {
                throw new RuntimeException("State file must be a JSON object: " + path);
            }
            return (Map<String, Object>) value;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeFile(String path, Object data) {
        try {
            Path p = Path.of(path);
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writeValue(p.toFile(), data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object parse(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
