package com.trade.algotrade.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.service.MongoDumpService;

import jakarta.websocket.server.PathParam;

@RestController
@Validated
@RequestMapping(value = "/api/v1/mongo")
@CrossOrigin(origins = "*")
public class MongoDumpController {
	
	@Autowired
	private MongoDumpService mongoDumpService;
	
	@GetMapping(value = "/{strategy}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void restoreStaticTable(@PathParam("strategy") String strategy){
		mongoDumpService.restoreStaticTable(strategy);
	}

}
