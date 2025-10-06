package com.fampay.scheduler.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.fampay.*")
public class JobSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobSchedulerApplication.class, args);
	}

}
