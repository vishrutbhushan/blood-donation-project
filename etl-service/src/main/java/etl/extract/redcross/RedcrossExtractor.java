package etl.extract.redcross;

import etl.constants.Constants;
import etl.util.JsonUtil;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class RedcrossExtractor {
    private final JsonUtil jsonUtil;
    private final RestClient restClient;

    public RedcrossExtractor(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
        this.restClient = RestClient.builder().build();
    }

    public Object fetchIncremental(long fromTs, long toTs) {
        log.info("api.enter redcross.source.incremental since={} until={}", fromTs, toTs);
        String url = Constants.REDCROSS_BASE_URL + Constants.REDCROSS_INCREMENTAL_ENDPOINT
                + "?" + Constants.REDCROSS_SINCE_PARAM + "=" + fromTs
                + "&" + Constants.REDCROSS_UNTIL_PARAM + "=" + toTs;
        String body = restClient.get().uri(url).retrieve().body(String.class);
        if (body == null) {
            throw new RuntimeException("redcross api returned empty body");
        }
        Object payload = jsonUtil.parse(body);
        log.info("api.exit redcross.source.incremental");
        return payload;
    }

    public Object fetchByDate(LocalDate date) {
        log.info("api.enter redcross.source.day date={}", date);
        String url = Constants.REDCROSS_BASE_URL + Constants.REDCROSS_INCREMENTAL_DAY_ENDPOINT
                + "?" + Constants.REDCROSS_DATE_PARAM + "=" + date;
        String body = restClient.get().uri(url).retrieve().body(String.class);
        if (body == null) {
            throw new RuntimeException("redcross day api returned empty body");
        }
        Object payload = jsonUtil.parse(body);
        log.info("api.exit redcross.source.day");
        return payload;
    }

    public Object fetchByMonth(YearMonth month) {
        log.info("api.enter redcross.source.month month={}", month);
        String url = Constants.REDCROSS_BASE_URL + Constants.REDCROSS_INCREMENTAL_MONTH_ENDPOINT
                + "?" + Constants.REDCROSS_MONTH_PARAM + "=" + month;
        String body = restClient.get().uri(url).retrieve().body(String.class);
        if (body == null) {
            throw new RuntimeException("redcross month api returned empty body");
        }
        Object payload = jsonUtil.parse(body);
        log.info("api.exit redcross.source.month");
        return payload;
    }
}
