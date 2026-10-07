package com.rebel.teach.config;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rebel.api.core.auth.User;
import com.rebel.api.core.event.Event;
import com.rebel.api.core.teach.TeachAdminController;
import com.rebel.api.core.teach.Teacher;
import com.rebel.teach.mapper.TeachServiceMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EventManager {

	private final TeachServiceMapper mapper;
	private final TeachAdminController controller;

	@Bean
	Consumer<Event<Integer, User>> teacherCreatedEvent() {

		return event -> {

			log.info("Received event: type={}", event.type());

			switch (event.type()) {

			case CREATE:

				log.info("Processing CREATE event");

				Teacher teacher = mapper.toDto(event.data());

				log.info("Creating teacher: {}", teacher);

				controller.createTeacher(teacher)
	            .subscribe(
	                    id -> log.info(
	                            "Teacher created successfully with id={}",
	                            id
	                    ),
	                    error -> log.error(
	                            "Error creating teacher",
	                            error
	                    )
	            );

				break;

			case DELETE:

			    log.info("Processing DELETE event");

			    Integer userId = event.key();

			    controller.deleteTeacher(userId)
			            .subscribe(
			                    unused -> log.info(
			                            "Teacher deleted successfully with userId={}",
			                            userId
			                    ),
			                    error -> log.error(
			                            "Error deleting teacher with userId={}",
			                            userId,
			                            error
			                    )
			            );

			    break;

			default:

				log.warn("Unsupported event type: {}", event.type());

				break;
			}
		};
	}
}
