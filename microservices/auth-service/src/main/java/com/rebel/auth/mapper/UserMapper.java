package com.rebel.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.rebel.api.core.auth.User;
import com.rebel.auth.entity.UserEntity;
import com.rebel.util.ServiceUtil;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected ServiceUtil serviceUtil;

    @Mapping(
        target = "serviceAddress",
        expression = "java(serviceUtil.getServiceAddress())"
    )
    public abstract User toDomain(UserEntity user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    public abstract UserEntity toEntity(User user);
}