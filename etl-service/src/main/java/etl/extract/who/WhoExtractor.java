package etl.extract.who;

import etl.constants.Constants;
import etl.util.JsonUtil;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class WhoExtractor {
    private final JsonUtil jsonUtil;
    private final RestClient restClient;

    public WhoExtractor(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
        this.restClient = RestClient.builder().build();
    }

    public Object fetchIncremental(long fromTs, long toTs) {
        log.info("api.enter who.source.incremental since={} until={}", fromTs, toTs);
        String url = Constants.WHO_BASE_URL + Constants.WHO_INCREMENTAL_ENDPOINT
                + "?" + Constants.WHO_SINCE_PARAM + "=" + fromTs
                + "&" + Constants.WHO_UNTIL_PARAM + "=" + toTs;
        String body = restClient.get().uri(url).retrieve().body(String.class);
        if (body == null) {
            throw new RuntimeException("who api returned empty body");
        }
        Object payload = jsonUtil.parse(body);
        log.info("api.exit who.source.incremental");
        return payload;
    }

    public Object fetchByDate(LocalDate date) {
        log.info("api.enter who.source.day date={}", date);
        String url = Constants.WHO_BASE_URL + Constants.WHO_INCREMENTAL_DAY_ENDPOINT
                + "?" + Constants.WHO_DATE_PARAM + "=" + date;
        String body = restClient.get().uri(url).retrieve().body(String.class);
        if (body == null) {
            throw new RuntimeException("who day api returned empty body");
        }
        Object payload = jsonUtil.parse(body);
        log.info("api.exit who.source.day");
        return payload;
    }

    public Object fetchByMonth(YearMonth month) {
        log.info("api.enter who.source.month month={}", month);
        String url = Constants.WHO_BASE_URL + Constants.WHO_INCREMENTAL_MONTH_ENDPOINT
                + "?" + Constants.WHO_MONTH_PARAM + "=" + month;
        String body = restClient.get().uri(url).retrieve().body(String.class);
        if (body == null) {
            throw new RuntimeException("who month api returned empty body");
        }
        Object payload = jsonUtil.parse(body);
        log.info("api.exit who.source.month");
        return payload;
    }
}
