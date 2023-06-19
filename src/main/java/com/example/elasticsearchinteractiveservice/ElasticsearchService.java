package com.example.elasticsearchinteractiveservice;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ElasticsearchService {

    private static final String INDEX_NAME = "companies";

    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public ElasticsearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public ResponseEntity<Company> searchCompanyByDomain(String domain) {
        try {
            GetResponse<Company> response = this.elasticsearchClient.get(getReqeustBuilder -> getReqeustBuilder
                            .index(INDEX_NAME)
                            .id(domain),
                    Company.class);
            if (response.found()) {
                return ResponseEntity.ok(response.source());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException ex) {
            log.error("Exception thrown while searching for data: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public List<Company> searchCompaniesByPhoneNumber(String phoneNumber) {
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
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (IOException ex) {
            log.error("Exception thrown while searching for data: {}", ex.getMessage());
            return List.of();
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
            List<Company> fetchedCompanies = response.docs().stream().map(item -> item.result().source()).toList();
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

    private List<Company> fetchCompanies() {
        Company company1 = new Company("domain value 1", "commercialName value 1", "legalName value 1", List.of("some name 1"), null, null, null);
        Company company2 = new Company("domain value 2", "commercialName value 2", "legalName value 2", List.of("some name 2"), null, null, null);
        return List.of(company1, company2);
    }

    private Company createCompany(Company company, CompanyUpdateRequest updateRequest) {
        return new Company(
                company.domain(),
                company.commercialName(),
                company.legalName(),
                company.allAvailableName(),
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