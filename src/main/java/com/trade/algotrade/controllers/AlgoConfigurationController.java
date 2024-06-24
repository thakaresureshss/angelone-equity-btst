package com.trade.algotrade.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trade.algotrade.request.ConfigurationRequest;
import com.trade.algotrade.response.ConfigurationResponse;
import com.trade.algotrade.service.AlgoConfigurationService;

import jakarta.servlet.http.HttpServletRequest;


/**
 * @author Rahul Pansare
 *
 */
@RestController
@Validated
@RequestMapping(value = "/api/v1")
@CrossOrigin(origins = "*")
public class AlgoConfigurationController {
	private static final Logger logger = LoggerFactory.getLogger(AlgoConfigurationController.class);
	
	@Autowired
	private AlgoConfigurationService algoConfigurationService;
	
	@PostMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigurationResponse> createConfiguration(@RequestBody ConfigurationRequest configurationRequest,
			HttpServletRequest request) {
		logger.debug(" **** [CREATE CONFIGURATION][REQUEST] ********* Request : {}", configurationRequest);
		ConfigurationResponse configurationResponse = algoConfigurationService.createConfiguration(configurationRequest);
		logger.debug(" **** [CREATE CONFIGURATION][RESPONSE]********* STRATEGY Response {}", configurationResponse);
		return new ResponseEntity<>(configurationResponse, HttpStatus.OK);
	}

	@GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigurationResponse> getConfiguration(HttpServletRequest request) {
		logger.debug(" **** [GET CONFIGURATION][REQUEST] ********* Request ");
		ConfigurationResponse configurationResponse = algoConfigurationService.getConfigurationDetails();
		logger.debug("**** [GET CONFIGURATION][RESPONSE]********* CONFIGURATION Response {}", configurationResponse);
		return new ResponseEntity<>(configurationResponse, HttpStatus.OK);
	}
	
	@PutMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigurationResponse> modifyStrategy(@RequestBody ConfigurationRequest configurationRequest,
			HttpServletRequest request) {
		logger.debug(" **** [MODIFY CONFIGURATION][REQUEST] *********  Request ");
		ConfigurationResponse configurationResponse = algoConfigurationService.modifyConfiguration(configurationRequest);
		logger.debug("**** [MODIFY CONFIGURATION][RESPONSE]********* CONFIGURATION Response {}", configurationResponse);
		return new ResponseEntity<>(configurationResponse, HttpStatus.OK);
	}
}
