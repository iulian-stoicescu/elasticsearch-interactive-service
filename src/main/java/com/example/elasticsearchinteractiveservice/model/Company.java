package com.example.elasticsearchinteractiveservice.model;

import java.util.List;

public record Company(
        String domain,
        String commercialName,
        String legalName,
        List<String> allAvailableName,
        List<String> phoneNumbers,
        List<String> socialMediaLinks,
        List<String> addresses
) {}
