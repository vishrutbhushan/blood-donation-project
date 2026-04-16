package etl.source;

import etl.constants.Constants;
import etl.extract.redcross.RedcrossExtractor;
import etl.model.EtlBatch;
import etl.transform.redcross.RedcrossTransformPipeline;
import etl.util.PincodeGeoMap;
import org.springframework.stereotype.Component;

@Component
public class RedcrossSourceHandler implements SourceHandler {
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
        return extractor.fetchIncremental(fromTs, toTs);
    }

    @Override
    public EtlBatch transform(Object payload, PincodeGeoMap geoMap) {
        return transformer.run(payload, geoMap);
    }
}
