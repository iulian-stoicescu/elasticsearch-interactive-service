package com.example.elasticsearchinteractiveservice.model;

import com.example.elasticsearchinteractiveservice.StringListDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public record CompanyPartialData(
        String domain,
        @JsonProperty("company_commercial_name")
        String commercialName,
        @JsonProperty("company_legal_name")
        String legalName,
        @JsonProperty("company_all_available_names")
        @JsonDeserialize(using = StringListDeserializer.class)
        List<String> allAvailableNames
) {}
