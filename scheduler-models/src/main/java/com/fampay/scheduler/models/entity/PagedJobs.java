package com.fampay.scheduler.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagedJobs {
    List<JobEntity> jobEntities;
    Long nextScheduledTimestamp;
    String nextJobId;
    boolean last;
}
