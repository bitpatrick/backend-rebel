package com.rebel.teach.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.rebel.teach.entity.LessonEntity;
import com.rebel.teach.entity.TeacherEntity;

public interface LessonRepository extends JpaRepository<LessonEntity, Integer> {

    /**
     * Recupera tutte le lezioni di un teacher paginated.
     */
    @Transactional(readOnly = true)
    Page<LessonEntity> findByTeacher(
            TeacherEntity teacher,
            Pageable pageable
    );
    
    @Transactional(readOnly = true)
    Page<LessonEntity> findByTeacherEmail(
			String email,
			Pageable pageable
	);

}