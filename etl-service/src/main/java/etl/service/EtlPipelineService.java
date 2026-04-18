package etl.service;

import etl.constants.Constants;
import etl.load.clickhouse.ClickhouseLoader;
import etl.load.elasticsearch.ElasticsearchLoader;
import etl.model.BloodBank;
import etl.model.Donor;
import etl.model.EtlBatch;
import etl.model.InventoryTransaction;
import etl.source.SourceHandler;
import etl.util.PincodeGeoMap;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EtlPipelineService {
    private final PincodeGeoMap pincodeGeoMap;
    private final ClickhouseLoader clickhouseLoader;
    private final ElasticsearchLoader elasticsearchLoader;
    private final EtlStateStore stateStore;
    private final List<SourceHandler> sourceHandlers;
    private final EtlBatchAccumulator accumulator = new EtlBatchAccumulator();

    private volatile boolean initialized;
    private volatile boolean bulkLoadDone;
    private volatile boolean bulkLoadRunning;

    public EtlPipelineService(
            PincodeGeoMap pincodeGeoMap,
            ClickhouseLoader clickhouseLoader,
            ElasticsearchLoader elasticsearchLoader,
            EtlStateStore stateStore,
            List<SourceHandler> sourceHandlers
    ) {
        this.pincodeGeoMap = pincodeGeoMap;
        this.clickhouseLoader = clickhouseLoader;
        this.elasticsearchLoader = elasticsearchLoader;
        this.stateStore = stateStore;
        this.sourceHandlers = sourceHandlers;
    }

    public synchronized String startInitialBulkLoad() {
        initializeIfNeeded();
        if (bulkLoadDone) {
            return "bulk load already completed";
        }
        if (bulkLoadRunning) {
            return "bulk load already running";
        }

        bulkLoadRunning = true;
        Thread worker = new Thread(() -> {
            try {
                runBulkLoad();
            } finally {
                synchronized (this) {
                    bulkLoadRunning = false;
                }
            }
        }, "etl-bulk-load");
        worker.setDaemon(true);
        worker.start();
        return "bulk load started";
    }

    public synchronized void runNightlyIncremental() {
        initializeIfNeeded();
        if (!bulkLoadDone || bulkLoadRunning) {
            return;
        }

        long now = System.currentTimeMillis();
        for (SourceHandler sourceHandler : sourceHandlers) {
            pullAndMerge(sourceHandler, stateStore.getLastSyncTs(sourceHandler.sourceName()), now);
        }

        flushToTargets();
        for (SourceHandler sourceHandler : sourceHandlers) {
            stateStore.setLastSyncTs(sourceHandler.sourceName(), now);
        }
        stateStore.save();
    }

    private synchronized void initializeIfNeeded() {
        if (initialized) {
            return;
        }
        stateStore.load();
        clickhouseLoader.ensureAnalyticsTables();
        elasticsearchLoader.bootstrap();
        bulkLoadDone = false;
        initialized = true;
    }

    private void runBulkLoad() {
        if (bulkLoadDone) {
            return;
        }

        accumulator.reset();
        long now = System.currentTimeMillis();
        for (SourceHandler sourceHandler : sourceHandlers) {
            pullAndMerge(sourceHandler, Constants.INITIAL_PULL_START_TS, now);
        }

        flushToTargets();
        for (SourceHandler sourceHandler : sourceHandlers) {
            stateStore.setLastSyncTs(sourceHandler.sourceName(), now);
        }
        stateStore.save();
        bulkLoadDone = true;
    }

    private void pullAndMerge(SourceHandler sourceHandler, long fromTs, long toTs) {
        Object payload = sourceHandler.fetchIncremental(fromTs, toTs);
        EtlBatch batch = sourceHandler.transform(payload, pincodeGeoMap);
        accumulator.merge(sourceHandler.sourceName(), batch);
    }

    private void flushToTargets() {
        List<BloodBank> banks = accumulator.collectLatestBanks();
        List<Donor> donors = accumulator.collectLatestDonors(banks);
        List<InventoryTransaction> pendingTransactions = accumulator.collectPendingInventoryTransactions();
        List<InventoryTransaction> currentInventoryState = accumulator.collectCurrentInventoryState();

        clickhouseLoader.loadBanks(banks);
        clickhouseLoader.loadDonors(donors);
        clickhouseLoader.loadInventoryTransactions(pendingTransactions);
        clickhouseLoader.batchAggregateInventoryDays();
        clickhouseLoader.batchAggregateDonorDays();
        elasticsearchLoader.loadBanks(banks, currentInventoryState);
        elasticsearchLoader.loadDonors(donors);

        accumulator.clearPendingInventoryTransactions();
    }
}