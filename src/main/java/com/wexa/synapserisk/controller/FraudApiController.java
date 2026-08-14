package com.wexa.synapserisk.controller;

import com.wexa.synapserisk.service.DataSeederService;
import com.wexa.synapserisk.service.FraudDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FraudApiController {

    private final FraudDetectionService fraudService;
    private final DataSeederService seederService;

    public FraudApiController(FraudDetectionService fraudService, DataSeederService seederService) {
        this.fraudService = fraudService;
        this.seederService = seederService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(fraudService.checkHealth());
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, String>> triggerSeed() {
        seederService.seedDatabase();
        return ResponseEntity.ok(Map.of("message", "Database successfully re-seeded!"));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> getAccounts() {
        return ResponseEntity.ok(fraudService.getAllAccounts());
    }

    @GetMapping("/investigate/paths/{accountId}")
    public ResponseEntity<List<Map<String, Object>>> getPaths(@PathVariable String accountId) {
        return ResponseEntity.ok(fraudService.traceMultiHopPaths(accountId));
    }

    @GetMapping("/investigate/fraud-rings")
    public ResponseEntity<List<Map<String, Object>>> getFraudRings() {
        return ResponseEntity.ok(fraudService.detectFraudRings());
    }

    @GetMapping("/investigate/shared-infrastructure")
    public ResponseEntity<List<Map<String, Object>>> getSharedInfra() {
        return ResponseEntity.ok(fraudService.detectSharedInfrastructure());
    }
}
