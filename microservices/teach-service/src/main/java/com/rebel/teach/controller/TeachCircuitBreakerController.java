package com.rebel.teach.controller;

import java.time.Duration;
import java.util.Random;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rebel.api.core.auth.User;
import com.rebel.api.exception.NotFoundException;
import com.rebel.teach.entity.TeacherEntity;
import com.rebel.teach.mapper.TeachMapper;
import com.rebel.teach.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/cb")
@RequiredArgsConstructor
public class TeachCircuitBreakerController {

    private final TeacherRepository teacherRepository;
    private final TeachMapper mapper;
    private final Random randomNumberGenerator = new Random();

    @GetMapping("/triesToRetrieveTeacher/{teacherId}")
    public Mono<User> triesToRetrieveTeacher(
    		@PathVariable int teacherId, 
			@RequestParam(value = "delay", required = false, defaultValue
			 = "0") int delay, 
			@RequestParam(value = "faultPercent", required = false, 
			 defaultValue = "0") int faultPercent
            ) {

        log.info("Received request: teacherId={}, delay={}s, faultPercent={}%",
                teacherId, delay, faultPercent);

        return Mono.fromCallable(() -> getTeacherFromDb(teacherId))
                .map(teacher -> throwErrorIfBadLuck(teacher, faultPercent))
                .delayElement(Duration.ofSeconds(delay))
                .map(mapper::toUserDto)
                .doOnSuccess(user -> log.info(
                        "Successfully retrieved teacher with id={}", teacherId))
                .doOnError(error -> log.error(
                        "Failed to retrieve teacher with id={}: {}",
                        teacherId, error.getMessage()));
    }

    private TeacherEntity getTeacherFromDb(int teacherId) {
        log.debug("Searching teacher with id={}", teacherId);

        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> {
                    log.warn("Teacher not found: id={}", teacherId);
                    return new NotFoundException(
                            "Teacher not found with id: " + teacherId);
                });
    }

    private TeacherEntity throwErrorIfBadLuck(
            TeacherEntity entity, int faultPercent) {

        if (faultPercent == 0) {
            log.debug("Error simulation disabled");
            return entity;
        }

        int randomThreshold = getRandomNumber(1, 100);

        if (faultPercent < randomThreshold) {
            log.debug("No simulated error: faultPercent={}, randomThreshold={}",
                    faultPercent, randomThreshold);
        } else {
            log.warn("Simulated error: faultPercent={}, randomThreshold={}",
                    faultPercent, randomThreshold);

            throw new RuntimeException("Something went wrong...");
        }

        return entity;
    }

    private int getRandomNumber(int min, int max) throws IllegalArgumentException {
        if (max < min) {
            throw new IllegalArgumentException(
                    "Max must be greater than min");
        }

        return randomNumberGenerator.nextInt((max - min) + 1) + min;
    }
    
}
