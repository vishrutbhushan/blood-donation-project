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

    @Scheduled(cron = "${ETL_ES_CRON:0 */5 * * * *}", zone = "${ETL_ZONE:Asia/Kolkata}")
    public void runElasticIncremental() {
        pipelineService.runElasticIncremental();
    }

    @Scheduled(cron = "${ETL_CH_CRON:0 0 0 * * *}", zone = "${ETL_ZONE:Asia/Kolkata}")
    public void runClickhouseIncremental() {
        pipelineService.runClickhouseDailyIncremental();
    }
}