package com.rebel.api.core.teach;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record LessonDto(
        Integer id,
        String name,
        
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate date,

        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime
) {
}