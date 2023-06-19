package com.example.elasticsearchinteractiveservice.model;

import java.util.List;

public record CompanyUpdateRequest(
        String domain,
        List<String> phoneNumbers,
        List<String> socialMediaLinks,
        List<String> addresses
) {
}
