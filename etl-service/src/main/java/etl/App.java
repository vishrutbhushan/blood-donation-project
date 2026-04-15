package etl;

import etl.constants.Constants;
import etl.load.clickhouse.ClickhouseLoader;
import etl.load.elasticsearch.ElasticsearchLoader;
import etl.model.BloodBank;
import etl.model.Donor;
import etl.model.EtlBatch;
import etl.source.SourceHandler;
import etl.util.JsonUtil;
import etl.util.PincodeGeoMap;
import etl.util.TimeUtil;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class App {
    private final JsonUtil json;
    private final PincodeGeoMap pincodeGeoMap;
    private final ClickhouseLoader clickhouseLoader;
    private final ElasticsearchLoader elasticsearchLoader;
    private final List<SourceHandler> sourceHandlers;

    private Map<String, Object> state;
    private final Map<String, Map<String, BloodBank>> banksBySource = new HashMap<>();
    private final Map<String, Map<String, Donor>> donorsBySource = new HashMap<>();
    private volatile boolean bulkLoadDone;

    public App(
            JsonUtil json,
            PincodeGeoMap pincodeGeoMap,
            ClickhouseLoader clickhouseLoader,
            ElasticsearchLoader elasticsearchLoader,
            List<SourceHandler> sourceHandlers
    ) {
        this.json = json;
        this.pincodeGeoMap = pincodeGeoMap;
        this.clickhouseLoader = clickhouseLoader;
        this.elasticsearchLoader = elasticsearchLoader;
        this.sourceHandlers = sourceHandlers;
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @PostConstruct
    public void onStart() {
        state = json.readFileMap(statePath());
        if (state == null) {
            state = new HashMap<>();
        }
        bootstrapElasticsearchWithRetry();
    }

    private void bootstrapElasticsearchWithRetry() {
        final int maxAttempts = 18;
        final long delayMs = 5000L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                elasticsearchLoader.bootstrap();
                return;
            } catch (RuntimeException ex) {
                if (attempt == maxAttempts) {
                    throw ex;
                }
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted while waiting to retry Elasticsearch bootstrap", ie);
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${ETL_INCREMENTAL_INTERVAL_MS:300000}")
    public synchronized void runIncremental() {
        if (!bulkLoadDone) {
            return;
        }
        long now = System.currentTimeMillis();
        for (SourceHandler handler : sourceHandlers) {
            pullAndProcess(handler, getLastSyncTs(handler.sourceName()), now);
        }
        flushToTargets();
        for (SourceHandler handler : sourceHandlers) {
            setLastSyncTs(handler.sourceName(), now);
        }
        saveState();
    }

    public synchronized String triggerBulkLoad() {
        if (bulkLoadDone) {
            return "bulk load already completed";
        }

        banksBySource.clear();
        donorsBySource.clear();

        long now = System.currentTimeMillis();
        for (SourceHandler handler : sourceHandlers) {
            pullAndProcess(handler, Constants.INITIAL_PULL_START_TS, now);
        }
        flushToTargets();
        for (SourceHandler handler : sourceHandlers) {
            setLastSyncTs(handler.sourceName(), now);
        }
        bulkLoadDone = true;
        saveState();
        return "bulk load completed";
    }

    private void pullAndProcess(SourceHandler handler, long fromTs, long toTs) {
        long cursor = fromTs;
        while (cursor < toTs) {
            long end = Math.min(cursor + Constants.INCREMENT_WINDOW_MS, toTs);
            Object payload = handler.fetchIncremental(cursor, end);
            EtlBatch batch = handler.transform(payload, pincodeGeoMap);
            mergeSourceBatch(handler.sourceName(), batch);
            cursor = end;
        }
    }

    private void mergeSourceBatch(String source, EtlBatch batch) {
        Map<String, BloodBank> bankMemory = banksBySource.computeIfAbsent(source, k -> new HashMap<>());
        Map<String, Donor> donorMemory = donorsBySource.computeIfAbsent(source, k -> new HashMap<>());

        for (BloodBank b : batch.getBanks()) {
            if (b.getBankId() != null && !b.getBankId().isBlank()) {
                bankMemory.put(b.getBankId(), b);
            }
        }
        for (Donor d : batch.getDonors()) {
            if (d.getDonorId() != null && !d.getDonorId().isBlank()) {
                donorMemory.put(d.getDonorId(), d);
            }
        }
    }

    private void flushToTargets() {
        List<BloodBank> allBanks = new ArrayList<>();
        for (Map<String, BloodBank> m : banksBySource.values()) {
            allBanks.addAll(m.values());
        }
        allBanks = mergeLatestBanks(allBanks);

        List<Donor> allDonors = new ArrayList<>();
        for (Map<String, Donor> m : donorsBySource.values()) {
            allDonors.addAll(m.values());
        }
        allDonors = mergeLatestDonors(allDonors);
        allDonors = resolveFk(allDonors, allBanks);

        clickhouseLoader.loadBanks(allBanks);
        clickhouseLoader.loadDonors(allDonors);
        elasticsearchLoader.loadBanks(allBanks);
        elasticsearchLoader.loadDonors(allDonors);
    }

    private List<BloodBank> mergeLatestBanks(List<BloodBank> records) {
        Map<String, BloodBank> merged = new LinkedHashMap<>();
        for (BloodBank r : records) {
            if (r.getBankId() == null || r.getSource() == null) {
                continue;
            }
            String key = r.getSource() + ":" + r.getBankId();
            BloodBank existing = merged.get(key);
            if (existing == null || !TimeUtil.toDateTime(r.getUpdatedAt()).isBefore(TimeUtil.toDateTime(existing.getUpdatedAt()))) {
                merged.put(key, r);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private List<Donor> mergeLatestDonors(List<Donor> records) {
        Map<String, Donor> merged = new LinkedHashMap<>();
        for (Donor r : records) {
            if (r.getDonorId() == null || r.getSource() == null) {
                continue;
            }
            String key = r.getSource() + ":" + r.getDonorId();
            Donor existing = merged.get(key);
            if (existing == null || !TimeUtil.toDateTime(r.getUpdatedAt()).isBefore(TimeUtil.toDateTime(existing.getUpdatedAt()))) {
                merged.put(key, r);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private List<Donor> resolveFk(List<Donor> donors, List<BloodBank> banks) {
        Map<String, String> bankByName = new HashMap<>();
        for (BloodBank b : banks) {
            if (b.getSource() != null && b.getBankName() != null && b.getBankId() != null) {
                bankByName.put(b.getSource() + ":" + b.getBankName(), b.getBankId());
            }
        }
        for (Donor d : donors) {
            if (d.getSource() != null && d.getLastDonatedBloodBank() != null) {
                String key = d.getSource() + ":" + d.getLastDonatedBloodBank();
                if (bankByName.containsKey(key)) {
                    d.setBankId(bankByName.get(key));
                }
            }
        }
        return donors;
    }

    private long getLastSyncTs(String source) {
        Object sourceState = state.get(source);
        if (sourceState instanceof Map) {
            Object val = ((Map<?, ?>) sourceState).get(Constants.KEY_LAST_SYNC);
            if (val != null) {
                return Long.parseLong(String.valueOf(val));
            }
        }
        return Constants.INITIAL_PULL_START_TS;
    }

    private void setLastSyncTs(String source, long ts) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(Constants.KEY_LAST_SYNC, String.valueOf(ts));
        state.put(source, map);
    }

    private void saveState() {
        json.writeFile(statePath(), state);
    }

    private String statePath() {
        return Constants.DATA_DIR + "/state.json";
    }
}
