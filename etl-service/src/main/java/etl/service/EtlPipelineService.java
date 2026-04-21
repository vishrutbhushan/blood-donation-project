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
import etl.util.TimeUtil;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

    public synchronized void runElasticIncremental() {
        initializeIfNeeded();
        if (!bulkLoadDone || bulkLoadRunning) {
            return;
        }

        accumulator.reset();
        long startedAt = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        for (SourceHandler sourceHandler : sourceHandlers) {
            pullAndMergeIncremental(sourceHandler, stateStore.getEsLastSyncTs(sourceHandler.sourceName()), now);
        }

        flushElasticsearch("incremental-es", startedAt, accumulator);
        for (SourceHandler sourceHandler : sourceHandlers) {
            stateStore.setEsLastSyncTs(sourceHandler.sourceName(), now);
        }
        stateStore.save();
    }

    public synchronized void runClickhouseDailyIncremental() {
        initializeIfNeeded();
        if (!bulkLoadDone || bulkLoadRunning) {
            return;
        }

        LocalDate day = LocalDate.now(ZoneId.of(Constants.ETL_ZONE)).minusDays(1);
        processClickhouseDay(day, "incremental-ch");
    }

    private synchronized void initializeIfNeeded() {
        if (initialized) {
            return;
        }
        stateStore.load();
        clickhouseLoader.ensureAnalyticsTables();
        elasticsearchLoader.bootstrap();
        bulkLoadDone = stateStore.isBulkDone();
        initialized = true;
    }

    private void runBulkLoad() {
        if (bulkLoadDone) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.of(Constants.ETL_ZONE));
        YearMonth currentMonth = YearMonth.from(today);
        EtlBatchAccumulator elasticBulkAccumulator = new EtlBatchAccumulator();

        for (int offset = 0; offset < Constants.BULK_MAX_MONTHS; offset++) {
            YearMonth month = currentMonth.minusMonths(offset);
            accumulator.reset();

            for (SourceHandler sourceHandler : sourceHandlers) {
                EtlBatch monthBatch = pullMonthBatch(sourceHandler, month);
                accumulator.merge(sourceHandler.sourceName(), monthBatch);
                elasticBulkAccumulator.merge(sourceHandler.sourceName(), monthBatch);
            }

            int monthRecords = processClickhouseMonth(month, "bulk-ch");
            if (monthRecords == 0) {
                break;
            }
        }

        long startedAt = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        flushElasticsearch("bulk-es", startedAt, elasticBulkAccumulator);

        for (SourceHandler sourceHandler : sourceHandlers) {
            stateStore.setEsLastSyncTs(sourceHandler.sourceName(), now);
        }
        stateStore.setBulkDone(true);
        stateStore.save();
        bulkLoadDone = true;
    }

    private int processClickhouseDay(LocalDate day, String batchType) {
        accumulator.reset();
        long startedAt = System.currentTimeMillis();
        for (SourceHandler sourceHandler : sourceHandlers) {
            pullAndMergeByDate(sourceHandler, day);
        }

        List<BloodBank> banks = accumulator.collectLatestBanks();
        List<Donor> donors = accumulator.collectLatestDonors(banks);
        List<InventoryTransaction> transactions = accumulator.collectPendingInventoryTransactions();
        int totalRead = banks.size() + donors.size() + transactions.size();
        if (totalRead == 0) {
            return 0;
        }

        clickhouseLoader.loadBanks(banks);
        clickhouseLoader.loadDonors(donors);
        clickhouseLoader.loadInventoryDay(day, transactions);
        clickhouseLoader.aggregateDonorDay(day);

        long endedAt = System.currentTimeMillis();
        recordAuditRows(batchType, "clickhouse", startedAt, endedAt, banks, donors, transactions, transactions.size());
        accumulator.clearPendingInventoryTransactions();
        return totalRead;
    }

    private int processClickhouseMonth(YearMonth month, String batchType) {
        long startedAt = System.currentTimeMillis();

        List<BloodBank> banks = accumulator.collectLatestBanks();
        List<Donor> donors = accumulator.collectLatestDonors(banks);
        List<InventoryTransaction> transactions = accumulator.collectPendingInventoryTransactions();
        int totalRead = banks.size() + donors.size() + transactions.size();
        if (totalRead == 0) {
            return 0;
        }

        clickhouseLoader.loadBanks(banks);
        clickhouseLoader.loadDonors(donors);

        Set<LocalDate> affectedDays = new TreeSet<>();
        for (InventoryTransaction transaction : transactions) {
            LocalDate day = TimeUtil.toDateTime(transaction.getEventTimestamp()).toLocalDate();
            if (YearMonth.from(day).equals(month)) {
                affectedDays.add(day);
            }
        }
        for (Donor donor : donors) {
            LocalDate day = TimeUtil.toDateTime(donor.getUpdatedAt()).toLocalDate();
            if (YearMonth.from(day).equals(month)) {
                affectedDays.add(day);
            }
        }

        for (LocalDate day : affectedDays) {
            clickhouseLoader.loadInventoryDay(day, transactions);
            clickhouseLoader.aggregateDonorDay(day);
        }

        long endedAt = System.currentTimeMillis();
        recordAuditRows(batchType, "clickhouse", startedAt, endedAt, banks, donors, transactions, transactions.size());
        accumulator.clearPendingInventoryTransactions();
        return totalRead;
    }

    private void flushElasticsearch(String batchType, long startedAt, EtlBatchAccumulator batchAccumulator) {
        List<BloodBank> banks = batchAccumulator.collectLatestBanks();
        List<Donor> donors = batchAccumulator.collectLatestDonors(banks);
        List<InventoryTransaction> currentInventoryState = batchAccumulator.collectCurrentInventoryState();
        List<InventoryTransaction> pendingTransactions = batchAccumulator.collectPendingInventoryTransactions();

        elasticsearchLoader.loadBanks(banks, currentInventoryState);
        elasticsearchLoader.loadDonors(donors);

        long endedAt = System.currentTimeMillis();
        recordAuditRows(batchType, "elasticsearch", startedAt, endedAt, banks, donors, pendingTransactions, currentInventoryState.size());
        batchAccumulator.clearPendingInventoryTransactions();
    }

    private void pullAndMergeIncremental(SourceHandler sourceHandler, long fromTs, long toTs) {
        Object payload = sourceHandler.fetchIncremental(fromTs, toTs);
        EtlBatch batch = sourceHandler.transform(payload, pincodeGeoMap);
        accumulator.merge(sourceHandler.sourceName(), batch);
    }

    private void pullAndMergeByDate(SourceHandler sourceHandler, LocalDate day) {
        Object payload = sourceHandler.fetchByDate(day);
        EtlBatch batch = sourceHandler.transform(payload, pincodeGeoMap);
        accumulator.merge(sourceHandler.sourceName(), batch);
    }

    private EtlBatch pullMonthBatch(SourceHandler sourceHandler, YearMonth month) {
        Object payload = sourceHandler.fetchByMonth(month);
        return sourceHandler.transform(payload, pincodeGeoMap);
    }

    private void recordAuditRows(
            String batchType,
            String target,
            long startedAt,
            long endedAt,
            List<BloodBank> banks,
            List<Donor> donors,
            List<InventoryTransaction> transactions,
            long inventoryStateRows) {
        Map<String, long[]> countsBySource = new HashMap<>();
        accumulateBankCounts(countsBySource, banks);
        accumulateDonorCounts(countsBySource, donors);
        accumulateTransactionCounts(countsBySource, transactions);

        long totalRead = 0L;
        long totalWritten = 0L;
        for (Map.Entry<String, long[]> entry : countsBySource.entrySet()) {
            long[] counts = entry.getValue();
            long rowsRead = counts[0];
            long rowsWritten = counts[1];
            totalRead += rowsRead;
            totalWritten += rowsWritten;
            clickhouseLoader.recordLoadAudit(
                batchType + "-" + startedAt + "-" + target + "-" + entry.getKey(),
                entry.getKey(),
                target,
                "etl_batch",
                startedAt,
                endedAt,
                rowsRead,
                rowsWritten,
                "success",
                "loaded");
        }

        clickhouseLoader.recordLoadAudit(
            batchType + "-" + startedAt + "-" + target + "-all",
            "all",
            target,
            "etl_batch",
            startedAt,
            endedAt,
            totalRead,
            totalWritten,
            "success",
            "inventory_state_rows=" + inventoryStateRows);
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
