package com.rebel.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.rebel.util.GlobalControllerExceptionHandler;
import com.rebel.util.ServiceUtil;

@SpringBootApplication
@Import({
	GlobalControllerExceptionHandler.class,
    ServiceUtil.class
})
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
