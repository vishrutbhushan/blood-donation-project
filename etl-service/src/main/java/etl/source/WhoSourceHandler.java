package etl.source;

import etl.constants.Constants;
import etl.extract.who.WhoExtractor;
import etl.model.EtlBatch;
import etl.transform.who.WhoTransformPipeline;
import etl.util.PincodeGeoMap;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

@Component
public class WhoSourceHandler implements SourceHandler {
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
        return extractor.fetchIncremental(fromTs, toTs);
    }

    @Override
    public Object fetchByDate(LocalDate date) {
        return extractor.fetchByDate(date);
    }

    @Override
    public Object fetchByMonth(YearMonth month) {
        return extractor.fetchByMonth(month);
    }

    @Override
    public EtlBatch transform(Object payload, PincodeGeoMap geoMap) {
        return transformer.run(payload, geoMap);
    }
}
