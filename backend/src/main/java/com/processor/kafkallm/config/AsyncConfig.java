package com.processor.kafkallm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita execucao assincrona com @Async.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
