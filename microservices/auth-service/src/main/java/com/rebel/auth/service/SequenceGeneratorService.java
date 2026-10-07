package com.rebel.auth.service;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.rebel.auth.entity.DatabaseSequence;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<Long> generateSequence(String sequenceName) {

        Query query = Query.query(
            Criteria.where("_id").is(sequenceName)
        );

        Update update = new Update()
            .inc("seq", 1);

        FindAndModifyOptions options =
            FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(true);

        return mongoTemplate
            .findAndModify(
                query,
                update,
                options,
                DatabaseSequence.class
            )
            .map(DatabaseSequence::getSeq);
    }
}