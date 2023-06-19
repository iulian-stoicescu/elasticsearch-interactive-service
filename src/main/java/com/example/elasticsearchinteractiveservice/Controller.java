package com.example.elasticsearchinteractiveservice;

import com.example.elasticsearchinteractiveservice.model.Company;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class Controller {

    private final ElasticsearchService elasticsearchService;

    @Autowired
    public Controller(ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @GetMapping("/domain")
    public List<Company> getData(@RequestParam("value") String domain) {
        return this.elasticsearchService.searchCompaniesByDomain(domain);
    }

    @PostMapping("/save")
    public void saveData() {
        this.elasticsearchService.persistInitialData();
    }
}
