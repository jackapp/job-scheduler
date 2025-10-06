package com.fampay.scheduler.api.controller;

import com.fampay.scheduler.api.adapter.ControllerAdapter;
import com.fampay.scheduler.api.dto.response.CreateJobResponse;
import com.fampay.scheduler.api.dto.response.JobExecutionHistoryResponse;
import com.fampay.scheduler.api.service.JobService;
import com.fampay.scheduler.api.dto.request.JobRequest;
import com.fampay.scheduler.api.dto.response.JobResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest jobRequest) {
        CreateJobResponse createJobResponse = jobService.createJob(ControllerAdapter.createJobRequestDto(jobRequest));
        return ResponseEntity.ok(ControllerAdapter.getJobResponseFromDto(createJobResponse));
    }

    @GetMapping("/job-executions/{jobId}")
    public ResponseEntity<JobExecutionHistoryResponse> getCompletedJobExecutions(@PathVariable("jobId") String jobId, @RequestParam(value = "limit",required = false) Integer limit) {
        return ResponseEntity.ok().body(ControllerAdapter.getJobExecutionHistoryResponseFromDto(jobService.getJobExecutionsForJobId(jobId,limit)));
    }
}
