package com.example.elasticsearchinteractiveservice.controller;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ElasticsearchException.class)
    public ResponseEntity<String> handleException(ElasticsearchException exception) {
        log.info("Handling ElasticsearchException: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    }
}
