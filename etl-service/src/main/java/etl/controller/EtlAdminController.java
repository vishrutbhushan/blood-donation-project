package etl.controller;

import etl.service.EtlPipelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/etl")
public class EtlAdminController {
    private final EtlPipelineService pipelineService;

    public EtlAdminController(EtlPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping("/bulk-load")
    public ResponseEntity<String> triggerBulkLoad() {
        String result = pipelineService.startInitialBulkLoad();
        return ResponseEntity.accepted().body(result);
    }
}