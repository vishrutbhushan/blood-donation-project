package etl.source;

import etl.constants.Constants;
import etl.extract.who.WhoExtractor;
import etl.model.EtlBatch;
import etl.transform.who.WhoTransformPipeline;
import etl.util.PincodeGeoMap;
import java.util.List;
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
    public EtlBatch transform(Object payload, PincodeGeoMap geoMap) {
        return transformer.run(payload, geoMap);
    }

    @Override
    public List<Object> inMemoryPayloads() {
        return extractor.getInMemoryPayloads();
    }
}
