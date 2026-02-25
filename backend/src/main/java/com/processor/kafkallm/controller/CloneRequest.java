package com.processor.kafkallm.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CloneRequest(
        @JsonProperty("id_key_processor") String idKeyProcessor, 
        @JsonProperty("repo_url") String repoUrl, 
        @JsonProperty("token") String token) {}
