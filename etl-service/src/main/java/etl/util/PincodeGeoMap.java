package etl.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import etl.model.GeoPoint;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PincodeGeoMap {
    private final Map<String, GeoPoint> geoMap;

    public PincodeGeoMap() {
        this.geoMap = loadFromResource();
    }

    public GeoPoint get(String pin) {
        if (pin == null) {
            return null;
        }
        return geoMap.get(pin.trim());
    }

    public Map<String, GeoPoint> asMap() {
        return geoMap;
    }

    private Map<String, GeoPoint> loadFromResource() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("pincode-map.json")) {
            if (is == null) {
                throw new IllegalStateException("pincode-map.json not found in ETL classpath");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            Map<String, GeoPoint> out = new HashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode node = entry.getValue();
                double lat = node.path("lat").asDouble(0.0);
                double lon = node.path("long").asDouble(0.0);
                out.put(entry.getKey(), new GeoPoint(lat, lon));
            });
            return out;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load pincode geo map", ex);
        }
    }
}
