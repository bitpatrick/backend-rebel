package com.rebel.teach;

import java.net.URI;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

public class CustomLoadBalancerConfiguration {

 	@Bean
    ServiceInstanceListSupplier serviceInstanceListSupplier(Environment environment) {

        // Recupera la porta dinamica di MockWebServer
        URI uri = URI.create(environment.getRequiredProperty("app.auth-service.url"));

        // Il test di successo deve raggiungere sempre il MockWebServer.
        // Gli errori si simulano con le risposte del mock, non con host inesistenti.
        ServiceInstance instance = new DefaultServiceInstance(
                "mock-auth", "fakehost", "localhost", uri.getPort(), false);
        
        return ServiceInstanceListSuppliers.from("fakehost", instance);
        
    }
	
    private void printInstance(ServiceInstance instance) {
        System.out.println("Instance ID: " + instance.getInstanceId());
        System.out.println("Service ID: " + instance.getServiceId());
        System.out.println("Host: " + instance.getHost());
        System.out.println("Port: " + instance.getPort());
        System.out.println("--------------------");
    }

	
}
