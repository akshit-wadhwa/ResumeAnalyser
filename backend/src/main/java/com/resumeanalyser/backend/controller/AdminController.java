package com.resumeanalyser.backend.controller;

import com.resumeanalyser.backend.dto.AdminSummaryDto;
import com.resumeanalyser.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryDto> summary() {
        return ResponseEntity.ok(adminService.summary());
    }
}
