package com.nkhang.lending.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "lending.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SchedulingConfig {
}
