package com.fampay.scheduler.api.dto.service;

import com.fampay.scheduler.models.dto.JobGuarantee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private String id;
    private String schedule;
    private ApiConfigDto apiConfig;
    private JobGuarantee type;
    private Long nextTimeStamp;
    private boolean active;
    private Long createdAt;
    private Long updatedAt;
}
