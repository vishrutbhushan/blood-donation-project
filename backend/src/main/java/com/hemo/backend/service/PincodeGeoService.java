package com.hemo.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PincodeGeoService {
    private final Map<String, GeoPoint> geoByPincode;

    public PincodeGeoService(ObjectMapper mapper) {
        this.geoByPincode = loadMap(mapper);
    }

    public Optional<GeoPoint> resolve(String pincode) {
        if (pincode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(geoByPincode.get(pincode.trim()));
    }

    private Map<String, GeoPoint> loadMap(ObjectMapper mapper) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("pincode-map.json")) {
            if (is == null) {
                throw new IllegalStateException("pincode-map.json not found in classpath");
            }

            JsonNode root = mapper.readTree(is);
            Map<String, GeoPoint> out = new HashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode node = entry.getValue();
                double lat = node.path("lat").asDouble(0.0);
                double lon = node.path("long").asDouble(0.0);
                out.put(entry.getKey(), new GeoPoint(lat, lon));
            });
            return out;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load pincode map", e);
        }
    }

    public record GeoPoint(double lat, double lon) {}
}
