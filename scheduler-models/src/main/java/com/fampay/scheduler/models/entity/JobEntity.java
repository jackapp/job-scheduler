package com.fampay.scheduler.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEntity {
    private String id;
    private String schedule;
    private ApiConfigEntity apiConfig;
    private String type;
    private Long nextScheduledTime;
    private boolean active;
    private Long createdAt;
    private Long updatedAt;


}
