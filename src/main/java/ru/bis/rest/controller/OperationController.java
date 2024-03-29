package ru.bis.rest.controller;

import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/operations")
@Slf4j
public class OperationController {

    private Random random = new Random();
    private Faker facker = new Faker();

    @Autowired
    private Cache operationCache;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Object>> createOperation(@RequestBody Map<String, Object> operation,
                                                               HttpServletRequest httpServletRequest) {
        log.info("Created operation :{}", operation);
        String uuid = UUID.randomUUID().toString();
        operation.put("id", uuid);
        operation.put("status", "running");
        operation.put("numberPlate", facker.regexify("[AHKMBCXTOPE][0-9]{3}[AHKMBCXTOPE]{2}[0-9]{2,3}"));
        operation.put("arriveAt", facker.date().future(8, TimeUnit.HOURS));
        operationCache.put(uuid, operation);
        return ResponseEntity
                .created(URI.create(
                        httpServletRequest.getRequestURL()
                                .append("/")
                                .append(uuid)
                                .toString())
                )
                .body(operation);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getOperations() {
        ConcurrentHashMap cache = (ConcurrentHashMap) operationCache.getNativeCache();
        List<Map<String, Object>> list = new ArrayList(cache.values());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{op-id}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable("op-id") String operationId) {
        Cache.ValueWrapper value = operationCache.get(operationId);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> operation = (Map<String, Object>) value.get();
        String status = (String) operation.get("status");
        if (status.equals("running")) {
            status = random.nextBoolean() ? "succeeded" : "running";
        }
        operation.replace("status", status);
        if (status.equals("succeeded")) {
            operation.put("pickUpAt", new Date());
        }
        operationCache.put(operationId, operation);
        return ResponseEntity.ok(operation);
    }

    @DeleteMapping("/{op-id}")
    public ResponseEntity<Map<String, Object>> cancelOperation(@PathVariable("op-id") String operationId) {
        Cache.ValueWrapper value = operationCache.get(operationId);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        operationCache.evict(operationId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{op-id}")
    public ResponseEntity<Map<String, Object>> patcheOperation(@PathVariable("op-id") String operationId,
                                                      @RequestBody Map<String, Object> operation) {
        Cache.ValueWrapper value = operationCache.get(operationId);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> operationMerged = new HashMap<>((Map<String, Object>) value.get());
        operationMerged.putAll(operation);
        operationCache.put(operationId, operationMerged);
        return ResponseEntity.ok(operationMerged);
    }

    @PutMapping("/warehouse/{op-id}")
    public ResponseEntity<Map<String, Object>> createPutWarehouse(@PathVariable("op-id") String operationId) {
        Cache.ValueWrapper value = operationCache.get(operationId);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> operation = (Map<String, Object>) value.get();
        operation.put("putWhStatus", "running");
        operationCache.put(operationId, operation);
        return ResponseEntity.ok(operation);
    }

    @GetMapping("/warehouse/{op-id}")
    public ResponseEntity<Map<String, Object>> getPutWarehouseStatus(@PathVariable("op-id") String operationId) {
        Cache.ValueWrapper value = operationCache.get(operationId);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> operation = (Map<String, Object>) value.get();
        String status = (String) operation.get("putWhStatus");
        if (status.equals("running")) {
            status = random.nextBoolean() ? "succeeded" : "running";
        }
        operation.replace("putWhStatus", status);
        if (status.equals("succeeded")) {
            operation.put("puttedAt", new Date());
        }
        operationCache.put(operationId, operation);
        return ResponseEntity.ok(operation);
    }
}
