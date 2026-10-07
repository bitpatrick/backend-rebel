package com.rebel.configserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	UserDetailsService createUserAdmin(PasswordEncoder passwordEncoder) {
		
		UserDetails user = User.builder()
				.username("root")
				.password(passwordEncoder.encode("s3cr3t"))
				.roles("USER")
				.build();
		
		return new InMemoryUserDetailsManager(user);
		
	}

	@Bean
    SecurityFilterChain sec(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                // HTTP Basic authentication
                .httpBasic(Customizer.withDefaults())

                .authorizeHttpRequests(req -> req
                        // Actuator accessibile senza autenticazione
                        .requestMatchers("/actuator/**").permitAll()

                        // Tutto il resto richiede autenticazione
                        .anyRequest().authenticated()
                );

        return http.build();
    }
	
}
