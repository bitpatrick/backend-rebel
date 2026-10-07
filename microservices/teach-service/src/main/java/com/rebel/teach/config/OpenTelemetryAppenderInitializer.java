package com.rebel.teach.config;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

@Component
@Profile("cloud")
class OpenTelemetryAppenderInitializer implements InitializingBean {

	private final OpenTelemetry openTelemetry;

	OpenTelemetryAppenderInitializer(OpenTelemetry openTelemetry) {
		this.openTelemetry = openTelemetry;
	}

	@Override
	public void afterPropertiesSet() {
		OpenTelemetryAppender.install(this.openTelemetry);
	}

}