package com.rebel.auth.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.rebel.auth.entity.UserEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveMongoRepository<UserEntity, Integer> {

    Mono<UserEntity> findByEmail(String email);
    
    Flux<UserEntity> findAllBy(Pageable pageable);
    
    Mono<Boolean> existsByEmail(String email);
    
}