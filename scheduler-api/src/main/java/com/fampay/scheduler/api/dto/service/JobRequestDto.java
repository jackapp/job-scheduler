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
public class JobRequestDto {
    private String schedule;
    private ApiConfigDto api;
    private JobGuarantee type;
    private String correlationId;
}
