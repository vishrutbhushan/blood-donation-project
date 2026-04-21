package etl.source;

import etl.constants.Constants;
import etl.extract.redcross.RedcrossExtractor;
import etl.model.EtlBatch;
import etl.transform.redcross.RedcrossTransformPipeline;
import etl.util.PincodeGeoMap;
import java.time.LocalDate;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RedcrossSourceHandler implements SourceHandler {
    private static final Logger log = LoggerFactory.getLogger(RedcrossSourceHandler.class);
    private final RedcrossExtractor extractor;
    private final RedcrossTransformPipeline transformer;

    public RedcrossSourceHandler(RedcrossExtractor extractor, RedcrossTransformPipeline transformer) {
        this.extractor = extractor;
        this.transformer = transformer;
    }

    @Override
    public String sourceName() {
        return Constants.SOURCE_REDCROSS;
    }

    @Override
    public Object fetchIncremental(long fromTs, long toTs) {
        log.info("api.enter redcross.source.fetchIncremental since={} until={}", fromTs, toTs);
        Object payload = extractor.fetchIncremental(fromTs, toTs);
        log.info("api.exit redcross.source.fetchIncremental");
        return payload;
    }

    @Override
    public Object fetchByDate(LocalDate date) {
        log.info("api.enter redcross.source.fetchByDate date={}", date);
        Object payload = extractor.fetchByDate(date);
        log.info("api.exit redcross.source.fetchByDate");
        return payload;
    }

    @Override
    public Object fetchByMonth(YearMonth month) {
        log.info("api.enter redcross.source.fetchByMonth month={}", month);
        Object payload = extractor.fetchByMonth(month);
        log.info("api.exit redcross.source.fetchByMonth");
        return payload;
    }

    @Override
    public EtlBatch transform(Object payload, PincodeGeoMap geoMap) {
        log.info("api.enter redcross.source.transform");
        EtlBatch batch = transformer.run(payload, geoMap);
        log.info("api.exit redcross.source.transform");
        return batch;
    }
}
