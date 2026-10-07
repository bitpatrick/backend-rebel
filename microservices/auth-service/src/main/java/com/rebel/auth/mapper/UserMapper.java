package com.rebel.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.rebel.api.core.auth.User;
import com.rebel.auth.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
	
    public User toDomain(UserEntity user);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
	public UserEntity toEntity(User user);
    
}

