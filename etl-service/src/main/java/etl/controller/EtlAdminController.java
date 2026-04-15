package etl.controller;

import etl.App;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/etl")
public class EtlAdminController {
    private final App app;

    public EtlAdminController(App app) {
        this.app = app;
    }

    @GetMapping("/bulk-load")
    public ResponseEntity<String> triggerBulkLoad() {
        return ResponseEntity.ok(app.triggerBulkLoad());
    }
}