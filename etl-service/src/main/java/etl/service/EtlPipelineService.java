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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        long startedAt = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        for (SourceHandler sourceHandler : sourceHandlers) {
            pullAndMerge(sourceHandler, stateStore.getLastSyncTs(sourceHandler.sourceName()), now);
        }

        flushToTargets("incremental", startedAt);
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
        long startedAt = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        for (SourceHandler sourceHandler : sourceHandlers) {
            pullAndMerge(sourceHandler, Constants.INITIAL_PULL_START_TS, now);
        }

        flushToTargets("bulk", startedAt);
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

    private void flushToTargets(String batchType, long startedAt) {
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

        long endedAt = System.currentTimeMillis();
        recordAuditRows(batchType, startedAt, endedAt, banks, donors, pendingTransactions, currentInventoryState);
        accumulator.clearPendingInventoryTransactions();
    }

    private void recordAuditRows(
            String batchType,
            long startedAt,
            long endedAt,
            List<BloodBank> banks,
            List<Donor> donors,
            List<InventoryTransaction> pendingTransactions,
            List<InventoryTransaction> currentInventoryState) {
        Map<String, long[]> countsBySource = new HashMap<>();
        accumulateBankCounts(countsBySource, banks);
        accumulateDonorCounts(countsBySource, donors);
        accumulateTransactionCounts(countsBySource, pendingTransactions);

        long totalRead = 0L;
        long totalWritten = 0L;
        for (Map.Entry<String, long[]> entry : countsBySource.entrySet()) {
            long[] counts = entry.getValue();
            long rowsRead = counts[0];
            long rowsWritten = counts[1];
            totalRead += rowsRead;
            totalWritten += rowsWritten;
            clickhouseLoader.recordLoadAudit(
                batchType + "-" + startedAt + "-" + entry.getKey(),
                entry.getKey(),
                "clickhouse/elasticsearch",
                "etl_batch",
                startedAt,
                endedAt,
                rowsRead,
                rowsWritten,
                "success",
                "banks+donors+inventory loaded");
        }

        long inventoryProjectionRows = currentInventoryState == null ? 0L : currentInventoryState.size();
        clickhouseLoader.recordLoadAudit(
            batchType + "-" + startedAt + "-all",
            "all",
            "clickhouse/elasticsearch",
            "etl_batch",
            startedAt,
            endedAt,
            totalRead,
            totalWritten,
            "success",
            "batch completed; inventory_state_rows=" + inventoryProjectionRows);
    }

    private void accumulateBankCounts(Map<String, long[]> countsBySource, List<BloodBank> banks) {
        if (banks == null) {
            return;
        }
        for (BloodBank bank : banks) {
            if (bank == null || bank.getSource() == null) {
                continue;
            }
            addCounts(countsBySource, bank.getSource(), 1L, 1L);
        }
    }

    private void accumulateDonorCounts(Map<String, long[]> countsBySource, List<Donor> donors) {
        if (donors == null) {
            return;
        }
        for (Donor donor : donors) {
            if (donor == null || donor.getSource() == null) {
                continue;
            }
            addCounts(countsBySource, donor.getSource(), 1L, 1L);
        }
    }

    private void accumulateTransactionCounts(Map<String, long[]> countsBySource, List<InventoryTransaction> transactions) {
        if (transactions == null) {
            return;
        }
        for (InventoryTransaction transaction : transactions) {
            if (transaction == null || transaction.getSource() == null) {
                continue;
            }
            addCounts(countsBySource, transaction.getSource(), 1L, 1L);
        }
    }

    private void addCounts(Map<String, long[]> countsBySource, String source, long readIncrement, long writtenIncrement) {
        long[] counts = countsBySource.computeIfAbsent(source, key -> new long[2]);
        counts[0] += readIncrement;
        counts[1] += writtenIncrement;
    }
}