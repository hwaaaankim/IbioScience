package com.dev.IbioScience;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class IbioScienceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IbioScienceApplication.class, args);
	}

}
