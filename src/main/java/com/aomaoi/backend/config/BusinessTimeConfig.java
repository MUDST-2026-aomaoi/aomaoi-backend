package com.aomaoi.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * The business runs on Thai time while containers usually run in UTC. Anything that decides
 * "what day is it" for the farm should use this clock (and tests can replace it with a fixed one).
 */
@Configuration
public class BusinessTimeConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock businessClock(@Value("${app.business-zone:Asia/Bangkok}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
