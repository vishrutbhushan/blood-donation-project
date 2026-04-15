package etl.controller;

import etl.App;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/etl")
@Slf4j
public class EtlAdminController {
    private final App app;

    public EtlAdminController(App app) {
        this.app = app;
    }

    @GetMapping("/bulk-load")
    public ResponseEntity<String> triggerBulkLoad() {
        log.info("etl bulk-load trigger requested");
        String result = app.triggerBulkLoad();
        log.info("etl bulk-load trigger result={}", result);
        return ResponseEntity.ok(result);
    }
}