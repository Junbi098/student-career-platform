package com.studentcareer.opportunity.service;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpportunityTimeConfiguration {
    @Bean
    public Clock opportunityClock() { return Clock.system(OpportunityRules.ZONE); }
}
