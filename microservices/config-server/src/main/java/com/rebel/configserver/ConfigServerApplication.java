package com.rebel.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.web.bind.annotation.GetMapping;

@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}
	
	@GetMapping("/hello")
	public String hello() {
	    System.out.println(getClass().getSimpleName() + " Hello endpoint called");
	    return "Hello from " + getClass().getSimpleName();
	}

}
