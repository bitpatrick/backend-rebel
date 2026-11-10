package com.rebel.teach;



import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/*
 * You can also use the @LoadBalancerClient annotation to pass your own load-balancer client configuration
 * 
 * https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html#custom-loadbalancer-configuration
 */
@LoadBalancerClient(name = "fakehost", configuration = CustomLoadBalancerConfiguration.class)
public class MyConfiguration {

    @Bean
    @LoadBalanced
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

}
