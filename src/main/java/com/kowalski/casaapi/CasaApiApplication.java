package com.kowalski.casaapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CasaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CasaApiApplication.class, args);
	}

}
