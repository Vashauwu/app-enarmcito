package com.empresa.medicina;

import org.springframework.boot.SpringApplication;

public class TestMedicinaApplication {

	public static void main(String[] args) {
		SpringApplication.from(MedicinaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
