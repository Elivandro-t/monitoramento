package com.example.projetoAP;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class ProjetoApApplication {

	public static void main(String[] args) {

		SpringApplication.run(ProjetoApApplication.class, args
		);
	}

}
