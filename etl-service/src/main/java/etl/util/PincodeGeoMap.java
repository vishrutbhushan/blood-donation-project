package etl.util;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PincodeGeoMap {
    private final Map<String, Object> geoMap;

    public PincodeGeoMap() {
        this.geoMap = new HashMap<>();
        this.geoMap.put("110001", point(28.6304, 77.2177));
        this.geoMap.put("400001", point(18.9388, 72.8354));
        this.geoMap.put("700001", point(22.5726, 88.3639));
        this.geoMap.put("560001", point(12.9762, 77.6033));
    }

    public Map<String, Object> asMap() {
        return geoMap;
    }

    private Map<String, Object> point(double lat, double lon) {
        Map<String, Object> p = new HashMap<>();
        p.put("lat", lat);
        p.put("lon", lon);
        return p;
    }
}
