package com.rebel.api.core.teach;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeacherRequest(
		
        @NotBlank(message = "Teacher name cannot be blank")
        String name,

        @NotBlank(message = "Teacher surname cannot be blank")
        String surname,

        @NotBlank(message = "Teacher email cannot be blank")
        @Email(message = "Teacher email must be a valid email address")
        String email,
        
        @Size(min = 8, max = 20, message = "Password must be at least 8 characters long")
        String password,
        
        String serviceAddress
        
) {
}