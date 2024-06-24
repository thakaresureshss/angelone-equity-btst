package com.trade.algotrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMongoRepositories
@EnableCaching
@EnableAutoConfiguration
@EnableFeignClients
public class AlgotradeApplication {

	public static void main(String[] args) {

		SpringApplication.run(AlgotradeApplication.class, args);
	}

}
