package com.rebel.discovery.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) {
	    
		http
	    	
	        .authorizeHttpRequests(auth -> auth
	        	.requestMatchers("/eureka/**").permitAll()
	            .requestMatchers("/actuator/**").permitAll()
	            .anyRequest().permitAll()
	        )
	        .csrf(AbstractHttpConfigurer::disable)
	    	.sessionManagement(AbstractHttpConfigurer::disable)
	    	.formLogin(AbstractHttpConfigurer::disable)
	    	.logout(AbstractHttpConfigurer::disable)
	    	.headers(AbstractHttpConfigurer::disable)
	        .httpBasic(AbstractHttpConfigurer::disable)
	        ;

	    return http.build();
	    
	}

}
