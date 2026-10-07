package com.rebel.teach.filter;

import org.springframework.data.jpa.domain.Specification;

import com.rebel.teach.entity.TeacherEntity;

/*
 * CB serve a costruire i vari pezzi della query
 * 
 * CQ serve a costruire una query il cui risultato sara TeacherEntity
 * 
 * Root ora dobbiamo dire da quale entity parte la query
 * equivale a FROM TEACHERS
 */
public class TeacherSpecifications {
	
	public static Specification<TeacherEntity> nameStartWith(String name) {

        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("name")),
                        name.toLowerCase() + "%"
                );
    }

    public static Specification<TeacherEntity> surnameStartWith(String surname) {

        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("surname")),
                        surname.toLowerCase() + "%"
                );
    }

}
