package etl.source;

import etl.model.EtlBatch;
import etl.util.PincodeGeoMap;

public interface SourceHandler {
    String sourceName();

    Object fetchIncremental(long fromTs, long toTs);

    EtlBatch transform(Object payload, PincodeGeoMap geoMap);
}
