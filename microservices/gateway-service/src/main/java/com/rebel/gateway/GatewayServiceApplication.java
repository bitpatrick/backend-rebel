package com.rebel.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayServiceApplication {
	
//	@Bean
//	ApplicationRunner logRoutes(RouteLocator routeLocator) {
//		
//	    return args -> routeLocator.getRoutes()
//	            .doOnNext(route ->
//	                    System.out.println(
//	                            "ROUTE: " + route.getId() +
//	                            " -> " + route.getUri()
//	                    )
//	            )
//	            .subscribe();
//	}
	
	public static void main(String[] args) {
		SpringApplication.run(GatewayServiceApplication.class, args);
	}

}