package com.example.elasticsearchinteractiveservice;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.elasticsearchinteractiveservice.model.Company;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
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

    public List<Company> searchCompaniesByDomain(String domain) {
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
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (IOException ex) {
            log.error("Exception thrown while searching for data: {}", ex.getMessage());
            return List.of();
        }
    }

    public void persistInitialData() {
        List<Company> companies = fetchCompanies();
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Company company : companies) {
            br.operations(operation -> operation.index(index -> index
                    .index(INDEX_NAME)
                    .id(company.domain())
                    .document(company))
            );
        }

        try {
            BulkResponse result = this.elasticsearchClient.bulk(br.build());

            if (result.errors()) {
                log.error("Bulk had errors");
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        log.error(item.error().reason());
                    }
                }
            }
        } catch (IOException ex) {
            log.error("Exception thrown while persisting data: {}", ex.getMessage());
        }
    }

    private List<Company> fetchCompanies() {
        Company company1 = new Company("domain value 1", "commercialName value 1", "legalName value 1", List.of("some name 1"), null, null, null);
        Company company2 = new Company("domain value 2", "commercialName value 2", "legalName value 2", List.of("some name 2"), null, null, null);
        return List.of(company1, company2);
    }
}