package com.audio.storage.controller;

import com.audio.storage.dto.StorageCreateRequest;
import com.audio.storage.dto.StorageCreateResponse;
import com.audio.storage.dto.StorageResponse;
import com.audio.storage.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/storages")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StorageCreateResponse> create(@Valid @RequestBody StorageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storageService.create(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StorageResponse>> getAll() {
        return ResponseEntity.ok(storageService.getAll());
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Long>> delete(@RequestParam String id) {
        return ResponseEntity.ok(storageService.deleteByIds(id));
    }
}
