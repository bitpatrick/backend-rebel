//package com.rebel.teach;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.time.ZonedDateTime;
//
//import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
//import org.springframework.cloud.stream.binder.test.EnableTestBinder;
//import org.springframework.cloud.stream.binder.test.InputDestination;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.MessageBuilder;
//
//import com.rebel.api.core.auth.User;
//import com.rebel.api.core.event.Event;
//import com.rebel.api.core.event.Type;
//import com.rebel.api.core.teach.Teacher;
//import com.rebel.api.exception.NotFoundException;
//import com.rebel.teach.mapper.TeachServiceMapper;
//import com.rebel.teach.repository.TeacherRepository;
//
//@EnableTestBinder
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@TestMethodOrder(OrderAnnotation.class)
//@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
//class TeachEventApplicationTests {
//
//    @Value("${spring.cloud.stream.bindings.teacherCreatedEvent-in-0.destination}")
//    String teacherEventDestination;
//
//    @Autowired
//    InputDestination inputDestination;
//
//    @Autowired
//    TeachServiceMapper teachMapper;
//
//    @Autowired
//    TeacherRepository teacherRepository;
//
//
//    @Test
//    @Order(1)
//    void shouldCreateTeacherWhenUserCreatedEventIsReceived() {
//
//        // given
//        User user = new User(
//                500,
//                "Luca",
//                "Bianchi",
//                "luca.bianchi@gmail.com",
//                null,
//                "TEACHER"
//        );
//
//        Event<Integer, User> event =
//                new Event<>(
//                        Type.CREATE,
//                        user.id(),
//                        user,
//                        ZonedDateTime.now()
//                );
//
//        Message<Event<Integer, User>> message =
//                MessageBuilder
//                        .withPayload(event)
//                        .setHeader("partitionKey", event.key())
//                        .build();
//
//        // when
//        inputDestination.send(message, teacherEventDestination);
//
//        // then
//        Teacher teacher = teacherRepository
//                .findByUserId(user.id())
//                .map(teachMapper::toDto)
//                .orElseThrow(NotFoundException::new);
//
//        assertThat(teacher.id()).isNotNull();
//        assertThat(teacher.email()).isEqualTo(user.email());
//        assertThat(teacher.name()).isEqualTo(user.name());
//        assertThat(teacher.surname()).isEqualTo(user.surname());
//        assertThat(teacher.userId()).isEqualTo(user.id());
//    }
//
//
//    @Test
//    @Order(2)
//    void shouldDeleteTeacherWhenUserDeletedEventIsReceived() {
//
//        // given
//        int userId = 500;
//
//        // Verifica che il teacher esista prima della cancellazione
//        assertThat(teacherRepository.existsByUserId(userId))
//                .isTrue();
//
//        Event<Integer, User> event =
//                new Event<>(
//                        Type.DELETE,
//                        userId,
//                        null,
//                        ZonedDateTime.now()
//                );
//
//        Message<Event<Integer, User>> message =
//                MessageBuilder
//                        .withPayload(event)
//                        .setHeader("partitionKey", event.key())
//                        .build();
//
//        // when
//        inputDestination.send(message, teacherEventDestination);
//
//        // then
//        assertThat(teacherRepository.existsByUserId(userId))
//                .isFalse();
//    }
//}