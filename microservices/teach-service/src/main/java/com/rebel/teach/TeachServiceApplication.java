package com.rebel.teach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.rebel.util.GlobalControllerExceptionHandler;
import com.rebel.util.ServiceUtil;

// Usiamo la factory reattiva. La configurazione bloccante di Spring Cloud 5.0.2
// fa ancora riferimento a classi Resilience4j per Boot 3 e va esclusa su Boot 4.
@SpringBootApplication(excludeName =
        "org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JAutoConfiguration")
//@SpringBootApplication
@Import({
	GlobalControllerExceptionHandler.class,
    ServiceUtil.class
})
public class TeachServiceApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(TeachServiceApplication.class, args);
	}
	
}
