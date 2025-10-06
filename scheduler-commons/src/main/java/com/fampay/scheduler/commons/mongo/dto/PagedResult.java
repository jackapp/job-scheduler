package com.fampay.scheduler.commons.mongo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult <T>{
    private List<T> results;
    private Map<String,Object> cursorMap;
    private boolean lastPage;
}
