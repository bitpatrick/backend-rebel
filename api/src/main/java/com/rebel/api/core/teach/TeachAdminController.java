package com.rebel.api.core.teach;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.rebel.api.exception.InvalidInputException;
import com.rebel.api.exception.NotFoundException;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import reactor.core.publisher.Mono;

public interface TeachAdminController {
	
	@GetMapping("/hello")
	@ResponseStatus(HttpStatus.OK)
	public Mono<String> hello();
	
	@GetMapping("/teachers")
	@ResponseStatus(HttpStatus.OK)
	public Mono<Page<Teacher>> teachers(
	        @RequestParam(required = false) String name,
	        @RequestParam(required = false) String surname,
	        Pageable pageable);
	
	@GetMapping("/teachers/{id}")
    @ResponseStatus(HttpStatus.OK)
	public Mono<Teacher> getTeacher(
            @PathVariable
            @Positive(message = "Teacher ID must be a positive integer")
            Integer id)
            throws NotFoundException;
	
	@PostMapping("/teachers")
    @ResponseStatus(HttpStatus.CREATED)
	public Mono<Integer> createTeacher(
            @Valid @RequestBody Teacher request)
            throws InvalidInputException;
	
	@PutMapping("/teachers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> updateTeacher(
            @PathVariable
            @Positive(message = "Teacher ID must be a positive integer")
            Integer id,
            @Valid @RequestBody Teacher request)
            throws NotFoundException, InvalidInputException;
	
	@DeleteMapping("/teachers/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
	public Mono<Void> deleteTeacher (
            @PathVariable
            @Positive(message = "UserId must be a positive integer")
            Integer userId) throws NotFoundException;
	
	@GetMapping("/teachers/{id}/lessons")
    @ResponseStatus(HttpStatus.OK)
	public Mono<Page<LessonDto>> getTeacherLessons(
            @PathVariable
            @Positive(message = "Teacher ID must be a positive integer")
            Integer id,
            Pageable pageable)
            throws NotFoundException, ConstraintViolationException;
	
	@GetMapping("/lessons/{id}")
    @ResponseStatus(HttpStatus.OK)
	public Mono<LessonDto> getLesson(
            @PathVariable
            @Positive(message = "Lesson ID must be a positive integer")
            Integer id)
            throws NotFoundException;

}
