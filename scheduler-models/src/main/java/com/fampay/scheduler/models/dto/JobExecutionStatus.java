package com.fampay.scheduler.models.dto;

import org.apache.commons.lang3.StringUtils;

public enum JobExecutionStatus {
    SCHEDULED,
    STARTED,
    FAILED,
    FINISHED;


    public static JobExecutionStatus from(String s) {
        for (JobExecutionStatus jobExecutionStatus : values()) {
            if (StringUtils.equals(jobExecutionStatus.name(),s)) {
                return jobExecutionStatus;
            }
        }
        return null;
    }

    public static boolean isTerminalState(JobExecutionStatus jobExecutionStatus) {
        return jobExecutionStatus== FINISHED || jobExecutionStatus == FAILED;
    }

    public static boolean hasStartedBefore(JobExecutionStatus jobExecutionStatus) {
        return jobExecutionStatus!=SCHEDULED;
    }

}
