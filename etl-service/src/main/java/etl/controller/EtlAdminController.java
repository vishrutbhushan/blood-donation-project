package etl.controller;

import etl.service.EtlPipelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/etl")
@Slf4j
public class EtlAdminController {
    private final EtlPipelineService pipelineService;

    public EtlAdminController(EtlPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping("/bulk-load")
    public ResponseEntity<String> triggerBulkLoad() {
        log.info("api.enter etl.bulk-load");
        String result = pipelineService.startInitialBulkLoad();
        log.info("api.exit etl.bulk-load result={}", result);
        return ResponseEntity.accepted().body(result);
    }
}