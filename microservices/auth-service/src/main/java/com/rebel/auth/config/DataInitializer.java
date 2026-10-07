package com.rebel.auth.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.rebel.auth.entity.UserEntity;
import com.rebel.auth.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
	
	public static List<UserEntity> INIT_USERS;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(ApplicationArguments args) {

        String password = passwordEncoder.encode("password");

        INIT_USERS = List.of(

                UserEntity.builder()
                        .id(999l)
                        .name("Mario")
                        .surname("Rossi")
                        .email("mario.rossi@rebel.com")
                        .password(password)
                        .role("ADMIN")
                        .build(),

                UserEntity.builder()
                        .id(805l)
                        .name("Giulia")
                        .surname("Romano")
                        .email("giulia.romano@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(806l)
                        .name("Andrea")
                        .surname("Ferrari")
                        .email("andrea.ferrari@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(807l)
                        .name("Elena")
                        .surname("Ricci")
                        .email("elena.ricci@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(808l)
                        .name("Davide")
                        .surname("Marino")
                        .email("davide.marino@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(809l)
                        .name("Sara")
                        .surname("Conti")
                        .email("sara.conti@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(810l)
                        .name("Matteo")
                        .surname("Gallo")
                        .email("matteo.gallo@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(811l)
                        .name("Francesca")
                        .surname("Costa")
                        .email("francesca.costa@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(812l)
                        .name("Stefano")
                        .surname("Fontana")
                        .email("stefano.fontana@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(813l)
                        .name("Valentina")
                        .surname("Moretti")
                        .email("valentina.moretti@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(814l)
                        .name("Simone")
                        .surname("De Luca")
                        .email("simone.deluca@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(815l)
                        .name("Lorenzo")
                        .surname("Rinaldi")
                        .email("lorenzo.rinaldi@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(816l)
                        .name("Martina")
                        .surname("Bianchi")
                        .email("martina.bianchi@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(817l)
                        .name("Alessandro")
                        .surname("Ferri")
                        .email("alessandro.ferri@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(818l)
                        .name("Chiara")
                        .surname("Esposito")
                        .email("chiara.esposito@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build(),

                UserEntity.builder()
                        .id(819l)
                        .name("Marco")
                        .surname("Santoro")
                        .email("marco.santoro@rebel.com")
                        .password(password)
                        .role("TEACHER")
                        .build()
        );

        // Insert users into MongoDB
        userRepository.saveAll(INIT_USERS)
                .subscribe();
    }
}
