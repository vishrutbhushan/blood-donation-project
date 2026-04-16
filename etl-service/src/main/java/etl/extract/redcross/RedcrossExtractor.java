package etl.extract.redcross;

import etl.constants.Constants;
import etl.util.JsonUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RedcrossExtractor {
    private final JsonUtil jsonUtil;
    private final RestClient restClient;

    public RedcrossExtractor(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
        this.restClient = RestClient.builder().build();
    }

    public Object fetchIncremental(long fromTs, long toTs) {
        String url = Constants.REDCROSS_BASE_URL + Constants.REDCROSS_INCREMENTAL_ENDPOINT
                + "?" + Constants.REDCROSS_SINCE_PARAM + "=" + fromTs
                + "&" + Constants.REDCROSS_UNTIL_PARAM + "=" + toTs;
        String body = restClient.get().uri(url).retrieve().body(String.class);
        if (body == null) {
            throw new RuntimeException("redcross api returned empty body");
        }
        return jsonUtil.parse(body);
    }
}
