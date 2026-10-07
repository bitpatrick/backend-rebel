package com.rebel.teach.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import com.rebel.teach.entity.TeacherEntity;

public interface TeacherRepository extends JpaRepository<TeacherEntity, Integer>, JpaSpecificationExecutor<TeacherEntity> {

	@Transactional(readOnly = true)
	Optional<TeacherEntity> findByEmail(String email);
	
	@Transactional(readOnly = true)
	boolean existsByEmail(String email);
	
	@Transactional(readOnly = true)
	boolean existsByUserId(int id);
	
	@Transactional(readOnly = false)
	int deleteByUserId(int id);

	@Transactional(readOnly = true)
	Optional<TeacherEntity> findByUserId(int id);
	
}
