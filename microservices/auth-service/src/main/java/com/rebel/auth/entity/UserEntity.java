package com.rebel.auth.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserEntity implements UserDetails {
	
	@Transient
    public static final String SEQUENCE_NAME = "users_sequence";

    @Id
    private Long id;

    @Field
    private String name;

    @Field
    private String surname;

    @Field
    private String email;

    @Field
    private String password;

    @Field
    private String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert role into a Spring Security authority
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getUsername() {
        return email;
    }
}