package com.sparta.springmsaorder.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class SchedulerConfig {
    @Bean(name = "specificTaskSchedulerBeanName")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("task-scheduler-");
        return scheduler;
    }
}
