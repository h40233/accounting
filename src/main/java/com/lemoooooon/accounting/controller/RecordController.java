package com.lemoooooon.accounting.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lemoooooon.accounting.dto.CategoryStatsDto;
import com.lemoooooon.accounting.dto.StatsDto;
import com.lemoooooon.accounting.model.Record;
import com.lemoooooon.accounting.repository.RecordRepository;
import com.lemoooooon.accounting.service.RecordService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/records") // 網址都是以 /api/records 開頭
public class RecordController {

    private final RecordService recordService;
    private final RecordRepository recordRepository;

    /**
     * 1. 新增記帳
     * 網址: POST /api/records?googleId=...&accountId=...
     * 注意：現在多了一個 accountId 參數
     */
    @PostMapping
    public Record createRecord(
            @RequestParam String googleId, 
            @RequestParam Long accountId, // 👈 新增這個參數
            @RequestBody Record record) {
        
        return recordService.createRecord(googleId, accountId, record);
    }

    /**
     * 2. 查詢某人的所有記帳
     * 網址: GET /api/records?googleId=123
     */
    @GetMapping
    public List<Record> getRecords(@RequestParam String googleId) {
        return recordService.getRecordsByMember(googleId);
    }

    /**
     * 3. 刪除記帳
     * 網址: DELETE /api/records/{id}?googleId=123
     * 例如: DELETE /api/records/5?googleId=my_google_id
     */
    @DeleteMapping("/{id}")
    public String deleteRecord(@PathVariable Long id, @RequestParam String googleId) {
        // @PathVariable: 從網址路徑抓變數 (/{id})
        recordService.deleteRecord(id, googleId);
        return "刪除成功！";
    }
    /**
     * 4. 修改記帳
     * 網址: PUT /api/records/{id}?googleId=...
     */
    @PutMapping("/{id}")
    public Record updateRecord(
            @PathVariable Long id, 
            @RequestParam String googleId, 
            @RequestBody Record record) { // 前端傳來的整包資料
        
        return recordService.updateRecord(id, googleId, record);
    }

    @GetMapping("/stats")
    public StatsDto getStats(@RequestParam String googleId) {
        return recordService.getStats(googleId);
    }

    @GetMapping("/stats/category")
    public List<CategoryStatsDto> getCategoryStats(
            @RequestParam String googleId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Record.RecordType type // 👈 Spring 會自動把字串轉 Enum
    ) {
        return recordService.getCategoryStats(googleId, startDate, endDate, type);
    }
}
