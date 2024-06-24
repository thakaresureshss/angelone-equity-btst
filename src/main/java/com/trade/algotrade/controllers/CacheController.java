package com.trade.algotrade.controllers;

import jakarta.websocket.server.PathParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Suresh Thakare
 * @since 20 June 2023
 */

@RestController
@Validated
@RequestMapping(value = "/api/cache")
@CrossOrigin(origins = "*")
public class CacheController {

    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

    @Autowired
    private CacheManager cacheManager;      // auto-wire cache manager

    // clear all cache using cache manager
    @DeleteMapping(value = "/clear/all")
    public ResponseEntity<Void> clearAll() {
        logger.info("Clearing all application cache");
        for (String name : cacheManager.getCacheNames()) {
            logger.info("Clearing Cache := {}", name);
            cacheManager.getCache(name).clear();            // clear cache by name
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(value = "/clear/{cacheName}")
    public ResponseEntity<Void> deleteCache(@PathParam("cacheName") String cacheName) {
        logger.info("Clearing Cache {} From Application", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        logger.info("Get Cache := {} From Application Value was := {}", cacheName, cache);
        cache.clear();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);// clear cache by name
    }

    @GetMapping(value = "/{cacheName}")
    public ResponseEntity<Cache> getCache(@PathParam("cacheName") String cacheName) {
        logger.info("Get Cache := {} From Application", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        return new ResponseEntity<>(cache, HttpStatus.OK);// clear cache by name
    }

    @GetMapping(value = "/all")
    public ResponseEntity<List<Cache>> getAllCache() {
        List<Cache> caches = new ArrayList<>();
        for (String name : cacheManager.getCacheNames()) {
            caches.add(cacheManager.getCache(name));
        }
        return new ResponseEntity<>(caches, HttpStatus.OK);
    }
}