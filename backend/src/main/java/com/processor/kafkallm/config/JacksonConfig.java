package com.processor.kafkallm.config;

import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonStreamConstraintsCustomizer(
            @Value("${spring.jackson.stream-read-constraints.max-string-length:100000000}")
                    int maxStringLength) {
        return builder -> {
            StreamReadConstraints constraints = StreamReadConstraints.builder()
                    .maxStringLength(maxStringLength)
                    .build();
            builder.factory(new JsonFactoryBuilder().streamReadConstraints(constraints).build());
        };
    }
}
