package com.banco.CajerosCardless;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.banco.CajerosCardless"})

public class CajerosCardlessApplication {

	public static void main(String[] args) {
		SpringApplication.run(CajerosCardlessApplication.class, args);
                System.out.println("Iniciando programa CajerosCardless");
	}
}