package com.example.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
public class ScheduledTask {

    private final Runnable runnable;

    private Long scheduledTime;

    private final Long period;

    private final Long delay;

    private final int taskType;

    private final TimeUnit unit;
}
