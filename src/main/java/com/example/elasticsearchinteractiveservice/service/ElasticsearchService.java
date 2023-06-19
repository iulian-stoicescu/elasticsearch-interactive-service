package com.example.elasticsearchinteractiveservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.elasticsearchinteractiveservice.model.Company;
import com.example.elasticsearchinteractiveservice.model.CompanyUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ElasticsearchService {

    private static final String INDEX_NAME = "companies";

    private final ElasticsearchClient elasticsearchClient;
    private final HelperService helperService;

    @Autowired
    public ElasticsearchService(ElasticsearchClient elasticsearchClient, HelperService helperService) {
        this.elasticsearchClient = elasticsearchClient;
        this.helperService = helperService;
    }

    public ResponseEntity<Company> searchCompanyByDomain(String domain) {
        try {
            SearchResponse<Company> response = this.elasticsearchClient.search(searchRequestBuilder -> searchRequestBuilder
                            .index(INDEX_NAME)
                            .query(queryBuilder -> queryBuilder
                                    .match(matchQueryBuilder -> matchQueryBuilder
                                            .field("domain")
                                            .query(domain)
                                            .fuzziness("AUTO")
                                    )
                            ),
                    Company.class
            );
            List<Company> companies = response.hits().hits().stream().map(Hit::source).toList();
            if (companies.size() > 0) {
                return ResponseEntity.ok(companies.get(0));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException ex) {
            log.error("Exception thrown while searchCompanyByDomain: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<Company> searchCompanyByPhoneNumber(String phoneNumber) {
        try {
            SearchResponse<Company> response = this.elasticsearchClient.search(searchRequestBuilder -> searchRequestBuilder
                            .index(INDEX_NAME)
                            .query(queryBuilder -> queryBuilder
                                    .term(termQueryBuilder -> termQueryBuilder
                                            .field("phoneNumbers")
                                            .value(phoneNumber)
                                    )
                            ),
                    Company.class
            );
            List<Company> companyList = response.hits().hits().stream().map(Hit::source).toList();
            if (companyList.size() > 0) {
                return ResponseEntity.ok(companyList.get(0));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException ex) {
            log.error("Exception thrown while searchCompanyByPhoneNumber: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public void updateCompanies(List<CompanyUpdateRequest> companyUpdateRequests) {
        Map<String, CompanyUpdateRequest> idToUpdateRequestMap = companyUpdateRequests.stream()
                .collect(Collectors.toMap(CompanyUpdateRequest::domain, Function.identity()));

        try {
            // first get the documents corresponding to the input ids
            MgetResponse<Company> response = this.elasticsearchClient.mget(mGetRequest ->
                    mGetRequest.index(INDEX_NAME).ids(List.copyOf(idToUpdateRequestMap.keySet())), Company.class);

            // map the response to a list of companies
            List<Company> fetchedCompanies = response.docs().stream().map(item -> item.result().source()).filter(Objects::nonNull).toList();
            BulkRequest.Builder br = new BulkRequest.Builder();

            // iterate over the list of companies and add an update operation for each one
            for (Company company : fetchedCompanies) {
                CompanyUpdateRequest updateRequest = idToUpdateRequestMap.get(company.domain());
                Company newCompany = createCompany(company, updateRequest);
                br.operations(operation -> operation.update(updateBuilder -> updateBuilder
                        .index(INDEX_NAME)
                        .id(newCompany.domain())
                        .action(updateAction -> updateAction.doc(newCompany)))
                );
            }

            // update the documents
            BulkResponse bulkResponse = this.elasticsearchClient.bulk(br.build());
            logBulkResponseErrors(bulkResponse);
        } catch (IOException ex) {
            log.error("Exception thrown while updating data: {}", ex.getMessage());
        }
    }

    public void persistInitialData() {
        List<Company> companies = fetchCompanies();
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Company company : companies) {
            br.operations(operation -> operation.index(indexBuilder -> indexBuilder
                    .index(INDEX_NAME)
                    .id(company.domain())
                    .document(company))
            );
        }

        try {
            BulkResponse bulkResponse = this.elasticsearchClient.bulk(br.build());
            logBulkResponseErrors(bulkResponse);
        } catch (IOException ex) {
            log.error("Exception thrown while persisting data: {}", ex.getMessage());
        }
    }

    public void deleteData(String domain) {
        try {
            this.elasticsearchClient.delete(deleteRequestBuilder -> deleteRequestBuilder.index(INDEX_NAME).id(domain));
        } catch (IOException ex) {
            log.error("Exception thrown while deleteData: {}", ex.getMessage());
        }
    }

    private List<Company> fetchCompanies() {
        return helperService.getCompaniesPartialData();
    }

    private Company createCompany(Company company, CompanyUpdateRequest updateRequest) {
        return new Company(
                company.domain(),
                company.commercialName(),
                company.legalName(),
                company.allAvailableNames(),
                updateRequest.phoneNumbers(),
                updateRequest.socialMediaLinks(),
                updateRequest.addresses()
        );
    }

    private void logBulkResponseErrors(BulkResponse response) {
        if (response.errors()) {
            log.error("Bulk had errors");
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    log.error(item.error().reason());
                }
            }
        }
    }
}
