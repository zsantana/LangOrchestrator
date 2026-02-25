package com.processor.kafkallm.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ListRepositoriesRequest(
        @JsonProperty("base_url") String baseUrl,
        @JsonProperty("token") String token) {}
