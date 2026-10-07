package com.rebel.api.core.teach;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Teacher(
		
		@Positive(message = "Teacher's id must be a positive integer")
		Integer id,
		
		@NotNull(message = "Teacher's userId cannot be null")
		@Positive(message = "Teacher's userId must be a positive integer")
		Integer userId,

        @NotBlank(message = "Teacher name cannot be blank")
        String name,

        @NotBlank(message = "Teacher surname cannot be blank")
        String surname,

        @NotBlank(message = "Teacher email cannot be blank")
        @Email(message = "Teacher email must be a valid email address")
        String email

) {
}