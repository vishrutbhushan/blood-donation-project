package etl.source;

import etl.constants.Constants;
import etl.extract.who.WhoExtractor;
import etl.model.EtlBatch;
import etl.transform.who.WhoTransformPipeline;
import etl.util.PincodeGeoMap;
import java.time.LocalDate;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WhoSourceHandler implements SourceHandler {
    private static final Logger log = LoggerFactory.getLogger(WhoSourceHandler.class);
    private final WhoExtractor extractor;
    private final WhoTransformPipeline transformer;

    public WhoSourceHandler(WhoExtractor extractor, WhoTransformPipeline transformer) {
        this.extractor = extractor;
        this.transformer = transformer;
    }

    @Override
    public String sourceName() {
        return Constants.SOURCE_WHO;
    }

    @Override
    public Object fetchIncremental(long fromTs, long toTs) {
        log.info("api.enter who.source.fetchIncremental since={} until={}", fromTs, toTs);
        Object payload = extractor.fetchIncremental(fromTs, toTs);
        log.info("api.exit who.source.fetchIncremental");
        return payload;
    }

    @Override
    public Object fetchByDate(LocalDate date) {
        log.info("api.enter who.source.fetchByDate date={}", date);
        Object payload = extractor.fetchByDate(date);
        log.info("api.exit who.source.fetchByDate");
        return payload;
    }

    @Override
    public Object fetchByMonth(YearMonth month) {
        log.info("api.enter who.source.fetchByMonth month={}", month);
        Object payload = extractor.fetchByMonth(month);
        log.info("api.exit who.source.fetchByMonth");
        return payload;
    }

    @Override
    public EtlBatch transform(Object payload, PincodeGeoMap geoMap) {
        log.info("api.enter who.source.transform");
        EtlBatch batch = transformer.run(payload, geoMap);
        log.info("api.exit who.source.transform");
        return batch;
    }
}
