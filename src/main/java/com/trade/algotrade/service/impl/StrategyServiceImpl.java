package com.trade.algotrade.service.impl;

import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.dto.EntryCondition;
import com.trade.algotrade.dto.ExitCondition;
import com.trade.algotrade.enitiy.StrategyEnity;
import com.trade.algotrade.exceptions.AlgoValidationException;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.exceptions.BusinessError;
import com.trade.algotrade.exceptions.ValidationError;
import com.trade.algotrade.repo.StrategyRepository;
import com.trade.algotrade.request.OrderConfigUpdateRequest;
import com.trade.algotrade.request.StrategyRequest;
import com.trade.algotrade.response.StrategyResponse;
import com.trade.algotrade.service.StrategyService;
import com.trade.algotrade.service.UserStrategyService;
import com.trade.algotrade.service.mapper.StrategyMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StrategyServiceImpl implements StrategyService {

	private final Logger logger = LoggerFactory.getLogger(StrategyServiceImpl.class);

	@Autowired
	StrategyRepository strategyRepository;

	@Autowired
	UserStrategyService userStrategyService;

	@Autowired
	StrategyMapper strategyMapper;

	@Override
	@CacheEvict(cacheNames = "strategies", allEntries = true)
	public StrategyResponse createStrategy(StrategyRequest strategyRequest) {
		logger.info("********** Creating new Strategy **********");
		StrategyEnity strategy = new StrategyEnity();
		validateStrategyRequest(strategyRequest);
		mapStrategyRequest(strategyRequest, strategy);
		strategy.setCreatedTime(LocalDateTime.now());
		strategy.setUpdatedTime(LocalDateTime.now());

		StrategyEnity savedStrategy = strategyRepository.save(strategy);
		StrategyResponse strategyResponse = new StrategyResponse();
		strategyResponse.setId(savedStrategy.getId());
		strategyResponse.setStrategyName(savedStrategy.getStrategyName());
		return strategyResponse;
	}

	private void validateStrategyRequest(StrategyRequest strategyRequest) {
		BusinessError validations = new BusinessError();
		validations.setStatus(HttpStatus.BAD_REQUEST);
		if (StringUtils.isEmpty(strategyRequest.getStrategyName())) {
			ValidationError violation = new ValidationError();
			violation.setField("strategyname");
			violation.setMessage("Can't be empty or null");
			violation.setRejectedValue(strategyRequest.getStrategyName());
			validations.getVoilations().add(violation);
		}

		if (strategyRequest.getEntryCondition() == null) {
			ValidationError violation = new ValidationError();
			violation.setField("entryCondition");
			violation.setMessage("Can't be empty or null");
			violation.setRejectedValue(strategyRequest.getStrategyName());
			validations.getVoilations().add(violation);
		}
		if (strategyRequest.getExitCondition() == null) {
			ValidationError violation = new ValidationError();
			violation.setField("exitCondition");
			violation.setMessage("Can't be empty or null");
			violation.setRejectedValue(strategyRequest.getStrategyName());
			validations.getVoilations().add(violation);
		}

		if (!CollectionUtils.isEmpty(validations.getVoilations())) {
			throw new AlgoValidationException(validations);
		}
		Optional<StrategyEnity> existingStrategy = strategyRepository
				.findByStrategyName(strategyRequest.getStrategyName());
		if (existingStrategy.isPresent()) {
			throw new AlgotradeException(ErrorCodeConstants.DUPLICATE_STRATEGY_FOUND);
		}
	}

	private void mapStrategyRequest(StrategyRequest strategyRequest, StrategyEnity strategy) {
		strategy.setStrategyName(strategyRequest.getStrategyName());
		EntryCondition entryCondition = new EntryCondition();
		entryCondition.setConditions(strategyRequest.getEntryCondition().getConditions());
		strategy.setEntryCondition(entryCondition);
		ExitCondition exitCondition = new ExitCondition();
		exitCondition.setConditions(strategyRequest.getExitCondition().getConditions());
		strategy.setExitCondition(exitCondition);
	}

	@Override

	@Cacheable(value = "strategies", key = "#strategyName")
	public StrategyResponse getStrategy(String strategyName) {
		logger.debug("Getting Strategy from DB  > {}", strategyName);
		Optional<StrategyEnity> existingStrategy = strategyRepository.findByStrategyName(strategyName);
		if (existingStrategy.isEmpty()) {
			throw new AlgotradeException(ErrorCodeConstants.STRATEGY_NOT_FOUND);
		}
		return strategyMapper.mapStrategyResponse(existingStrategy.get());
	}

	@Override
	@CachePut(value = "strategies", key = "#strategyName")
	public StrategyResponse modifyStrategy(String strategyName, StrategyRequest strategyRequest) {
		logger.info("Modifying Strategy from DB  > {}", strategyName);
		Optional<StrategyEnity> existingStrategy = strategyRepository.findByStrategyName(strategyName);
		if (existingStrategy.isEmpty()) {
			throw new AlgotradeException(ErrorCodeConstants.STRATEGY_NOT_FOUND);
		}
		StrategyEnity strategy = existingStrategy.get();
		mapStrategyRequest(strategyRequest, strategy);
		strategy.setUpdatedTime(LocalDateTime.now());
		return strategyMapper.mapStrategyResponse(strategyRepository.save(strategy));
	}

	@Override
	@CacheEvict(cacheNames = "strategies", allEntries = true)
	public void deleteStrategy(String strategyName) {
		logger.info("Deleting Strategy from DB > {}", strategyName);
		Optional<StrategyEnity> existingStrategy = strategyRepository.findByStrategyName(strategyName);
		if (existingStrategy.isEmpty()) {
			throw new AlgotradeException(ErrorCodeConstants.STRATEGY_NOT_FOUND);
		}
		strategyRepository.delete(existingStrategy.get());
	}

	@Override
	public List<StrategyEnity> getAllStrategies() {
		return strategyRepository.findAll();
	}

	@Override
	public void upadteOrderConfig(String strategyName, OrderConfigUpdateRequest orderConfigUpdateRequest) {

	}

}
