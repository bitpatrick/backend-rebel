package com.rebel.api.core.teach;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.rebel.api.exception.InvalidInputException;
import com.rebel.api.exception.NotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import reactor.core.publisher.Mono;

public interface TeacherController {
	
	@GetMapping("/hello")
	@ResponseStatus(HttpStatus.OK)
	public Mono<String> hello();

    @GetMapping("/teachers/me")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Teacher> me(@AuthenticationPrincipal Jwt jwt)
            throws NotFoundException;

    @GetMapping("/teachers/me/lessons")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Page<LessonDto>> myLessons(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable);

    @PostMapping("/registerLesson")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Integer> submitLessonDateTime(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LessonDateTimeRequest request)
            throws InvalidInputException, NotFoundException;

    @PutMapping("/lessons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<Void> updateLesson(
            @PathVariable
            @Positive(message = "Lesson ID must be a positive integer")
            Integer id,
            @Valid @RequestBody LessonDateTimeRequest request)
            throws NotFoundException, InvalidInputException;

    @DeleteMapping("/lessons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteLesson(
            @PathVariable
            @Positive(message = "Lesson ID must be a positive integer")
            Integer id)
            throws NotFoundException;


    @GetMapping("/lesson/types")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Set<String>> getLessonTypes();
    
}
