package com.rebel.teach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Component
public class ThreadPollConfig {
	
	@Value("${app.threadPoolSize:10}") 
	private int threadPoolSize;
	
	@Value("${app.taskQueueSize:100}")
	private int taskQueueSize;
	
	@Bean
	Scheduler jdbcScheduler() {
	 return Schedulers.newBoundedElastic(threadPoolSize,
	 taskQueueSize, "jdbc-pool");
	}

}
