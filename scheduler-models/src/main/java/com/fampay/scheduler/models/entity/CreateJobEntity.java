package com.fampay.scheduler.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobEntity {
    private String id;
    private String schedule;
    private ApiConfigEntity apiConfig;
    private String type;
    private String correlationId;
}
