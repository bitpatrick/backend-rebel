package com.rebel.api.core.teach;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;

public record LessonDateTimeRequest(
		@NotBlank(message = "Name cannot be blank")
		String name,
		@PastOrPresent(message = "Date must be in the past or present")
		LocalDate date, 
		LocalTime startTime, 
		LocalTime endTime) {
	
}
