package com.rebel.teach.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.rebel.api.core.auth.User;
import com.rebel.api.core.teach.LessonDateTimeRequest;
import com.rebel.api.core.teach.LessonDto;
import com.rebel.api.core.teach.Teacher;
import com.rebel.teach.entity.LessonEntity;
import com.rebel.teach.entity.TeacherEntity;

@Mapper(componentModel = "spring")
public interface TeachServiceMapper {

    // =========================
    // TEACHER
    // =========================

    Teacher toDto(TeacherEntity teacher);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    TeacherEntity toEntity(Teacher request);


    // =========================
    // LESSON
    // =========================

    LessonDto toDto(LessonEntity lesson);

    /*
     * Converte una richiesta di creazione in una nuova LessonEntity.
     *
     * Non viene impostato il teacher perché viene assegnato
     * direttamente dal service in base al JWT.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacher", ignore = true)
    LessonEntity toEntity(LessonDateTimeRequest request);

    /*
     * Aggiornamento parziale della LessonEntity esistente.
     *
     * Se un campo del request è null, MapStruct NON modifica
     * il valore già presente nell'entity.
     *
     * Esempio:
     *
     * request.date = nuova data
     * request.name = null
     *
     * => viene modificata solo la data.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "id", ignore = true)
    LessonEntity updateLessonFromRequest(
            LessonDateTimeRequest request,
            @MappingTarget LessonEntity lesson
    );


    // =========================
    // CUSTOM MAPPINGS
    // =========================

    default String toString(LessonEntity entity) {
        return entity.getName();
    }
    
    default LessonEntity createLesson(LessonDateTimeRequest request, TeacherEntity teacher) {
    	return LessonEntity.builder().name(request.name()).date(request.date())
    			.startTime(request.startTime()).endTime(request.endTime()).teacher(teacher).build();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "userId")
    Teacher toDto(User user);
	
}