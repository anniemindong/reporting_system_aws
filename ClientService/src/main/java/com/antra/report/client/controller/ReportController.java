package com.antra.report.client.controller;

import com.antra.report.client.exception.RequestNotFoundException;
import com.antra.report.client.pojo.FileType;
import com.antra.report.client.pojo.reponse.ErrorResponse;
import com.antra.report.client.pojo.reponse.GeneralResponse;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

@RestController
@EnableRetry
public class ReportController {
    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/report")
    public ResponseEntity<GeneralResponse> listReport() {
        log.info("Got Request to list all report");
        return ResponseEntity.ok(new GeneralResponse(reportService.getReportList()));
    }

    @PostMapping("/report/sync")
    @Retryable(value = {MethodArgumentNotValidException.class}, maxAttempts = 5, backoff = @Backoff(delay = 3000))
    public ResponseEntity<GeneralResponse> createReportDirectly(@RequestBody @Validated ReportRequest request) {
        log.info("Got Request to generate report - sync: {}", request);
        String test = request.getDescription();
        request.setDescription(String.join(" - ", "Sync", request.getDescription()));
        return ResponseEntity.ok(new GeneralResponse(reportService.generateReportsSync(request)));
    }

    @PostMapping("/report/async")
    @Retryable(value = {MethodArgumentNotValidException.class}, maxAttempts = 5, backoff = @Backoff(delay = 3000))
    public ResponseEntity<GeneralResponse> createReportAsync(@RequestBody @Validated ReportRequest request) {
        log.info("Got Request to generate report - async: {}", request);
        request.setDescription(String.join(" - ", "Async", request.getDescription()));
        reportService.generateReportsAsync(request);
        //ReportVO test = reportService.generateReportsAsync(request);
        return ResponseEntity.ok(new GeneralResponse());
    }

    @GetMapping("/report/content/{reqId}/{type}")
    @Retryable(value = {IOException.class}, maxAttempts = 5, backoff = @Backoff(delay = 3000))
    public void downloadFile(@PathVariable String reqId, @PathVariable FileType type, HttpServletResponse response) throws IOException {
        log.debug("Got Request to Download File - type: {}, reqid: {}", type, reqId);
        InputStream fis = reportService.getFileBodyByReqId(reqId, type);
        String fileType = null;
        String fileName = null;
        if(type == FileType.PDF) {
            fileType = "application/pdf";
            fileName = "report.pdf";
        } else if (type == FileType.EXCEL) {
            fileType = "application/vnd.ms-excel";
            fileName = "report.xls";
        }
        response.setHeader("Content-Type", fileType);
        response.setHeader("fileName", fileName);
        if (fis != null) {
            FileCopyUtils.copy(fis, response.getOutputStream());
        } else{
            response.setStatus(500);
        }
        log.debug("Downloaded File:{}", reqId);
    }

    @PostMapping("/report/{reqId}")
    public void deleteFile(@PathVariable(value = "reqId") String reqId) {
        log.info("delete report");
        reportService.deleteFile(reqId);
    }

//   @DeleteMapping
//   @PutMapping


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @Recover
    public ResponseEntity<GeneralResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Input Data invalid: {}", e.getMessage());
        String errorFields = e.getBindingResult().getFieldErrors().stream().map(fe -> String.join(" ",fe.getField(),fe.getDefaultMessage())).collect(Collectors.joining(", "));
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST, errorFields), HttpStatus.BAD_REQUEST);
    }
}
