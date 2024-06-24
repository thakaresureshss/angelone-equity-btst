package com.trade.algotrade.controllers;

import com.trade.algotrade.enitiy.config.TimezoneConfigEntity;
import com.trade.algotrade.repo.config.TimezoneConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.PathParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.List;

/**
 * This class acts as a controller to handle request related to Country.
 *
 * @author suresh.thakare
 */

@RestController
@Validated
@RequestMapping(value = "/api/v1/timezone/config")
@CrossOrigin(origins = "*")
public class TimezoneConfigController {
    private static final Logger logger = LoggerFactory.getLogger(TimezoneConfigController.class);

    @Autowired
    TimezoneConfigRepository timezoneConfigRepository;


    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TimezoneConfigEntity> addTimezoneConfig(@RequestBody TimezoneConfigEntity timezoneConfigRequest,
                                                                  HttpServletRequest request) {
        logger.info(" **** [ADD TimezoneConfig][REQUEST] ********* Request : {}", timezoneConfigRequest);
        TimezoneConfigEntity timezoneConfigEntity = timezoneConfigRepository.save(timezoneConfigRequest);
        logger.info("**** [ADD TimezoneConfig]********* Response {}", timezoneConfigEntity);
        return new ResponseEntity<>(timezoneConfigEntity, HttpStatus.OK);
    }


    @PostMapping(value = "/multiple",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TimezoneConfigEntity>> allMultipleConfig(@RequestBody List<TimezoneConfigEntity> timezoneConfigRequest,
                                                                  HttpServletRequest request) {
        logger.info(" **** [ADD  Multiple TimezoneConfig][REQUEST] ********* Request : {}", timezoneConfigRequest);
        List<TimezoneConfigEntity> timezoneConfigEntities = timezoneConfigRepository.saveAll(timezoneConfigRequest);
        logger.info("**** [ADD TimezoneConfig]********* Response {}", timezoneConfigEntities);
        return new ResponseEntity<>(timezoneConfigEntities, HttpStatus.OK);
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TimezoneConfigEntity>> modifyOrder(@PathParam("userId") String userId,
                                                                  HttpServletRequest request) {
        logger.info(" **** [GET ALL TimezoneConfig][REQUEST] *********");
        List<TimezoneConfigEntity> allTimezoneConfig = timezoneConfigRepository.findAll();
        logger.info("**** [GET ALL TimezoneConfig][RESPONSE]********* Total TimezoneConfig {} Found ", allTimezoneConfig);
        return new ResponseEntity<>(allTimezoneConfig, HttpStatus.OK);
    }

    @DeleteMapping(value = "/{configKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteTimezoneConfig(@PathParam("configKey") String configKey,
                                                     HttpServletRequest request) {
        logger.info(" **** [DELETE TimezoneConfig][REQUEST] ********* configKey: {}", configKey);
        timezoneConfigRepository.deleteByTimezoneKeyAndTimezone(configKey, Clock.systemDefaultZone().toString());
        logger.info("**** [DELETE TimezoneConfig][RESPONSE]*********");
        return new ResponseEntity<>(HttpStatus.OK);
    }

}