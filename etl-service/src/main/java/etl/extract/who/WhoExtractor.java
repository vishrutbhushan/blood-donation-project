package etl.extract.who;

import etl.constants.Constants;
import etl.util.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WhoExtractor {
    private final JsonUtil jsonUtil;
    private final RestClient restClient;
    private final List<Object> inMemoryPayloads = new ArrayList<>();

    public WhoExtractor(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
        this.restClient = RestClient.builder().build();
    }

    public Object fetchIncremental(long fromTs, long toTs) {
        String url = Constants.WHO_BASE_URL + Constants.WHO_INCREMENTAL_ENDPOINT
                + "?" + Constants.WHO_SINCE_PARAM + "=" + fromTs
                + "&" + Constants.WHO_UNTIL_PARAM + "=" + toTs;
        String body = restClient.get().uri(url).retrieve().body(String.class);
        if (body == null) {
            throw new RuntimeException("who api returned empty body");
        }
        Object payload = jsonUtil.parse(body);
        inMemoryPayloads.add(payload);
        return payload;
    }

    public List<Object> getInMemoryPayloads() {
        return inMemoryPayloads;
    }
}
