package com.fampay.scheduler.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JobTypeProcessorFactory {

    private final Map<String, JobTypeProcessor> processorMap = new HashMap<>();

    @Autowired
    public JobTypeProcessorFactory(List<JobTypeProcessor> processors) {
        for (JobTypeProcessor jobTypeProcessor : processors) {
            processorMap.put(jobTypeProcessor.getTypeProcessor().name(),jobTypeProcessor);
        }
    }

    public JobTypeProcessor getProcessor(String type) {
        return processorMap.get(type);
    }
}
