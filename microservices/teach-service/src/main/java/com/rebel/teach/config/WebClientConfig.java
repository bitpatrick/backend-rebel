package com.rebel.teach.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * In order to leverage additional capabilities that Spring Boot provides for
 * WebClient.Builder (for example, observability support) you may want to use
 * the autoconfigured WebClientCustomizer beans while creating the @LoadBalanced
 * WebClient.Builder beans
 * 
 * https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html#webclinet-loadbalancer-client
 */
@Configuration
@Profile("!local")
public class WebClientConfig {

	@Bean
	@LoadBalanced
	public WebClient.Builder webClientBuilder(ObjectProvider<WebClientCustomizer> customizers) {

		WebClient.Builder builder = WebClient.builder();

		customizers.orderedStream().forEach(customizer -> customizer.customize(builder));

		return builder;
	}
	
}
