package com.bank.bian.workforcetraining.api;

import com.bank.bian.workforcetraining.model.ControlRecord;
import com.bank.bian.workforcetraining.service.ControlRecordStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * BIAN semantic API for the "Workforce Training" service domain.
 *
 * Endpoints follow the BIAN action-term style:
 *   GET  /v1/service-domain                          → who am I (SD metadata)
 *   POST /v1/training-program-operating-session/initiate                    → Initiate a control record
 *   GET  /v1/training-program-operating-session                             → Retrieve (list)
 *   GET  /v1/training-program-operating-session/{crId}/retrieve             → Retrieve (single)
 *   PUT  /v1/training-program-operating-session/{crId}/update               → Update
 *   PUT  /v1/training-program-operating-session/{crId}/control              → Control (suspend|resume|terminate)
 */
@RestController
@RequestMapping("/v1")
public class ServiceDomainController {

    private final ControlRecordStore store;

    public ServiceDomainController(ControlRecordStore store) {
        this.store = store;
    }

    @GetMapping("/service-domain")
    public Map<String, String> serviceDomain() {
        return Map.of(
                "serviceDomain", "Workforce Training",
                "businessArea", "Business Support",
                "businessDomain", "Human Resources",
                "functionalPattern", "Operate",
                "assetType", "Training Program",
                "controlRecord", "Training Program Operating Session",
                "version", "0.1.0",
                "phase", "1-shallow"
        );
    }

    @PostMapping("/training-program-operating-session/initiate")
    @CircuitBreaker(name = "serviceDomain")
    public ResponseEntity<ControlRecord> initiate(@RequestBody(required = false) Map<String, Object> properties) {
        return ResponseEntity.status(HttpStatus.CREATED).body(store.initiate(properties));
    }

    @GetMapping("/training-program-operating-session")
    public Collection<ControlRecord> list() {
        return store.list();
    }

    @GetMapping("/training-program-operating-session/{crId}/retrieve")
    public ResponseEntity<ControlRecord> retrieve(@PathVariable String crId) {
        return store.retrieve(crId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/training-program-operating-session/{crId}/update")
    public ResponseEntity<ControlRecord> update(@PathVariable String crId,
                                                @RequestBody Map<String, Object> properties) {
        return store.update(crId, properties)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/training-program-operating-session/{crId}/control")
    public ResponseEntity<?> control(@PathVariable String crId,
                                     @RequestBody Map<String, String> body) {
        try {
            return store.control(crId, body.get("action"))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
