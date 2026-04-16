package etl.scheduler;

import etl.service.EtlPipelineService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EtlIncrementalScheduler {
    private final EtlPipelineService pipelineService;

    public EtlIncrementalScheduler(EtlPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @Scheduled(cron = "${ETL_INCREMENTAL_CRON:0 0 0 * * *}", zone = "${ETL_INCREMENTAL_ZONE:Asia/Kolkata}")
    public void runNightlyIncremental() {
        pipelineService.runNightlyIncremental();
    }
}