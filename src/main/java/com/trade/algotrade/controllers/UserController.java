package com.trade.algotrade.controllers;

import java.util.List;

import com.trade.algotrade.enitiy.UserStrategyEntity;
import com.trade.algotrade.service.OrderService;
import com.trade.algotrade.service.UserStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.trade.algotrade.request.UserRequest;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.PathParam;

@RestController
@Validated
@RequestMapping(value = "/api/v1")
@CrossOrigin(origins = "*")
public class UserController {
	
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	
	@Autowired
	private UserService userService;

	@Autowired
	private OrderService orderService;

	@GetMapping(value = "/user/all", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<UserResponse>> getUsers() {
		logger.info("**** [GET USER][REQUEST] ********* ");
		List<UserResponse> userResponseList = userService.getUsers();
		logger.info("**** [GET USER][RESPONSE]********* User Response {}", userResponseList);
		return new ResponseEntity<>(userResponseList, HttpStatus.OK);
	}
	
	@PostMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest userRequest,
			HttpServletRequest request) throws Exception {
		logger.info("**** [CREATE USER][REQUEST] ********* Request : {}", userRequest);
		UserResponse userResponse = userService.createUser(userRequest);
		logger.info("**** [CREATE USER][RESPONSE]********* User Response {}", userResponse);
		return new ResponseEntity<>(userResponse, HttpStatus.OK);
	}
	
	@PatchMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UserResponse> updateUser(@RequestBody UserRequest userRequest,
			HttpServletRequest request) throws Exception {
		logger.info("**** [UPDATE USER][REQUEST] ********* Request : {}", userRequest);
		UserResponse userResponse = userService.updateUser(userRequest);
		logger.info("**** [UPDATE USER][RESPONSE]********* User Response {}", userResponse);
		return new ResponseEntity<>(userResponse, HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/user/delete", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> deleteUser(@PathParam("userId") Long userId) {
		logger.info(" **** [DELETE USER][REQUEST] ********* ");
		userService.deleteUser(userId);
		logger.info("**** [DELETE USER][RESPONSE]********* ");
		return new ResponseEntity<>("Success", HttpStatus.OK);
	}

	@GetMapping(value = "/user/details", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Integer> getUsersDetails(@RequestParam String userId, @RequestParam String strategy) {
		logger.info("**** [GET USER Details][REQUEST] ********* ");
		Integer integer = orderService.getOrderQuantityForStrategy(userId,strategy);
		logger.info("**** [GET USER Details][RESPONSE]********* User Response {}", integer);
		return new ResponseEntity<>(integer, HttpStatus.OK);
	}
	
}
