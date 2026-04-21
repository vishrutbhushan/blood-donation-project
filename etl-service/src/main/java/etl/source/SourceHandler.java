package etl.source;

import etl.model.EtlBatch;
import etl.util.PincodeGeoMap;
import java.time.LocalDate;
import java.time.YearMonth;

public interface SourceHandler {
    String sourceName();

    Object fetchIncremental(long fromTs, long toTs);

    Object fetchByDate(LocalDate date);

    Object fetchByMonth(YearMonth month);

    EtlBatch transform(Object payload, PincodeGeoMap geoMap);
}
