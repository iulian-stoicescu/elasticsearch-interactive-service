package com.example.elasticsearchinteractiveservice.service;

import com.example.elasticsearchinteractiveservice.model.Company;
import com.example.elasticsearchinteractiveservice.model.CompanyPartialData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.asm.TypeReference;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class HelperService {

    public List<Company> getCompaniesPartialData() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<CompanyPartialData> companyPartialDataList = Arrays.asList(mapper.readValue(TypeReference.class.getResourceAsStream("/static/companies.json"), CompanyPartialData[].class));
            return companyPartialDataList.stream().map(this::createCompany).toList();
        } catch (IOException e) {
            log.warn(e.getMessage());
            log.warn("File companies.json could not be read!");
            return List.of();
        }
    }

    public Company createCompany(CompanyPartialData companyPartialData) {
        return new Company(
                companyPartialData.domain(),
                companyPartialData.commercialName(),
                companyPartialData.legalName(),
                companyPartialData.allAvailableNames(),
                null,
                null,
                null
        );
    }
}
