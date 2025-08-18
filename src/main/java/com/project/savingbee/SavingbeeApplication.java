package com.project.savingbee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SavingbeeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SavingbeeApplication.class, args);
	}

}
