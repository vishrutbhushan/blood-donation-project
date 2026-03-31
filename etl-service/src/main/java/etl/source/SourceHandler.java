package etl.source;

import etl.model.EtlBatch;
import java.util.List;
import java.util.Map;

public interface SourceHandler {
    String sourceName();

    Object fetchIncremental(long fromTs, long toTs);

    EtlBatch transform(Object payload, Map<String, Object> geoMap);

    List<Object> inMemoryPayloads();
}
