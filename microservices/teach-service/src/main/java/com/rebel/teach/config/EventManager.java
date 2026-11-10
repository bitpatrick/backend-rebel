package com.rebel.teach.config;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rebel.api.core.auth.User;
import com.rebel.api.core.event.Event;
import com.rebel.api.core.teach.TeachAdminController;
import com.rebel.api.core.teach.Teacher;
import com.rebel.teach.mapper.TeachMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EventManager {

    private final TeachMapper mapper;
    private final TeachAdminController controller;

    @Bean
    Consumer<Event<Integer, User>> teacherEvent() {

        return event -> {

            log.info("Received event: type={}", event.type());

            switch (event.type()) {

                case CREATE:

                    log.info("Processing CREATE event");

                    Teacher teacher = mapper.toTeacherDto(event.data());

                    log.info("Creating teacher: {}", teacher);
                    
                    // Blocca il thread fino al completamento della creazione, 
                    // propagando eventuali errori per consentire a Spring Cloud Stream 
                    // di gestirli tramite retry e dead-letter queue (RabbitMQ).
                    Integer id = controller.createTeacher(teacher).block();

                    log.info("Teacher created successfully with id={}", id);

                    break;

                case DELETE:

                    log.info("Processing DELETE event");

                    Integer userId = event.key();
                    	
                	// Blocca il thread fino al completamento della cancellazione, 
                    // propagando eventuali errori per consentire a Spring Cloud Stream 
                    // di gestirli tramite retry e dead-letter queue (RabbitMQ).
                    controller.deleteTeacher(userId).block();

                    log.info("Teacher deleted successfully with userId={}", userId);

                    break;

                default:

                    log.warn("Unsupported event type: {}", event.type());
                    break;
            }
        };
    }
}
