package com.quizzar;

import com.quizzar.storage.config.S3Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableRetry
@EnableCaching
@EnableAsync
@EnableConfigurationProperties(S3Properties.class)
public class QuizzarApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuizzarApiApplication.class, args);
	}

}
