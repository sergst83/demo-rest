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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/operations")
@Slf4j
public class OperationController {

    private Random random = new Random();
    private Faker facker = new Faker();

    @Autowired
    private Cache operationCache;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, Object> createOperation(@RequestBody Map<String, Object> operation) {
        log.info("Created operation :{}", operation);
        String uuid = UUID.randomUUID().toString();
        operation.put("id", uuid);
        operation.put("status", "running");
        operationCache.put(uuid, operation);
        return operation;
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
        operationCache.put(operationId, operation);
        return ResponseEntity.ok(Collections.singletonMap("status", status));
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
        operation.put("id", operationId);
        operationCache.put(operationId, operation);
        return ResponseEntity.ok().build();
    }
}
