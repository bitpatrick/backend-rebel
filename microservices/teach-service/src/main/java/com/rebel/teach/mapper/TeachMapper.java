package com.rebel.teach.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import com.rebel.api.core.auth.User;
import com.rebel.api.core.teach.CreateTeacherRequest;
import com.rebel.api.core.teach.LessonDateTimeRequest;
import com.rebel.api.core.teach.LessonDto;
import com.rebel.api.core.teach.Teacher;
import com.rebel.teach.entity.LessonEntity;
import com.rebel.teach.entity.TeacherEntity;
import com.rebel.util.ServiceUtil;

@Mapper(componentModel = "spring")
public abstract class TeachMapper {
	
	@Autowired
    protected ServiceUtil serviceUtil;
	
	// =========================
    // Lesson ENTITY -> Lesson DTO
    // =========================

    public abstract LessonDto toLessonDto(LessonEntity lesson);

    @BeanMapping(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "id", ignore = true)
    public abstract LessonEntity updateLessonFromRequest(
        LessonDateTimeRequest request,
        @MappingTarget LessonEntity lesson
    );
    
    // =========================
    // Teacher ENTITY -> Teacher DTO
    // =========================

    @Mapping(
        target = "serviceAddress",
        expression = "java(serviceUtil.getServiceAddress())"
    )
    public abstract Teacher toTeacherDto(TeacherEntity teacher);
    
    // =========================
    // Teacher ENTITY -> User DTO
    // =========================
    
    @Mapping(target = "id", source = "userId")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", constant = "TEACHER")
    @Mapping(
        target = "serviceAddress",
        expression = "java(serviceUtil.getServiceAddress())"
    )
    public abstract User toUserDto(TeacherEntity entity);
    
    // =========================
    // Teacher DTO -> Teacher ENTITY
    // =========================
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    public abstract TeacherEntity toTeacherEntity(Teacher teacher);
    
    // =========================
    // User -> Teacher ENTITY
    // =========================
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "lessons", ignore = true)
    public abstract TeacherEntity toTeacherEntity(User user);
    
    // =========================
    // User DTO -> Teacher DTO
    // =========================
    
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "userId")
    public abstract Teacher toTeacherDto(User user);

    // =========================
    // REQUESTs TO DTOs
    // =========================
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "TEACHER")
    @Mapping(
        target = "serviceAddress",
        expression = "java(serviceUtil.getServiceAddress())"
    )
    public abstract User toUserDto(CreateTeacherRequest request);
    
    // =========================
    // CUSTOM MAPPINGS
    // =========================

    public LessonEntity createLesson(
        LessonDateTimeRequest request,
        TeacherEntity teacher
    ) {
        return LessonEntity.builder()
            .name(request.name())
            .date(request.date())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .teacher(teacher)
            .build();
    }
    
}
