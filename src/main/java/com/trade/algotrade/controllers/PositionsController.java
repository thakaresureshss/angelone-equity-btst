package com.trade.algotrade.controllers;

import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.service.PositionService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * This class acts as a controller to handle request related to Country.
 * 
 * @author suresh.thakare
 */

@RestController
@Validated
@RequestMapping(value = "/api/v1/positions")
@CrossOrigin(origins = "*")
public class PositionsController {
	private static final Logger logger = LoggerFactory.getLogger(PositionsController.class);

	@Autowired
	PositionService positionService;

	@GetMapping(value = "/open/squareoff/all/fno", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> squareOffAllFnoPositions( HttpServletRequest request) {
		logger.info(" **** [SQUAREOFF ALL FNO OPEN POSITIONS][REQUEST] *********");
		positionService.squareOffOpenPositions(Segment.FNO);
		logger.info("**** [SQUAREOFF ALL FNO OPEN POSITIONS][RESPONSE]*********");
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "/open/squareoff/all/equity", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> squareOffAllEquityPositions( HttpServletRequest request) {
		logger.info(" **** [SQUAREOFF ALL EQUITY OPEN POSITIONS][REQUEST] *********");
		positionService.squareOffOpenPositions(Segment.EQ);
		logger.info("**** [SQUAREOFF ALL EQUITYOPEN POSITIONS][RESPONSE]*********");
		return new ResponseEntity<>(HttpStatus.OK);
	}

}