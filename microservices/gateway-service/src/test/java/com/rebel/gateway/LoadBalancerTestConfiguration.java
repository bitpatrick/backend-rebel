//package com.rebel.gateway;
//
//import java.util.List;
//
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.cloud.client.DefaultServiceInstance;
//import org.springframework.cloud.client.ServiceInstance;
//import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
//import org.springframework.context.annotation.Bean;
//
//import com.github.tomakehurst.wiremock.WireMockServer;
//import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
//
//import reactor.core.publisher.Flux;
//
//@TestConfiguration
//class LoadBalancerTestConfiguration {
//
//	@Bean
//	WireMockServer authServer() {
//		WireMockServer server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
//
//		server.start();
//		return server;
//	}
//
//	@Bean
//	ServiceInstanceListSupplier authServiceSupplier(WireMockServer authServiceMock) {
//
//		return new ServiceInstanceListSupplier() {
//
//			@Override
//			public String getServiceId() {
//				return "AUTH-SERVICE";
//			}
//
//			@Override
//			public Flux<List<ServiceInstance>> get() {
//
//				ServiceInstance instance = new DefaultServiceInstance("auth-service-test", "AUTH-SERVICE", "localhost",
//						authServiceMock.port(), false);
//
//				return Flux.just(List.of(instance));
//			}
//		};
//	}
//}