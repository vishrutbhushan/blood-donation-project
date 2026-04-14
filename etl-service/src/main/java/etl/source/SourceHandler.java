package etl.source;

import etl.model.EtlBatch;
import etl.util.PincodeGeoMap;
import java.util.List;

public interface SourceHandler {
    String sourceName();

    Object fetchIncremental(long fromTs, long toTs);

    EtlBatch transform(Object payload, PincodeGeoMap geoMap);

    List<Object> inMemoryPayloads();
}
