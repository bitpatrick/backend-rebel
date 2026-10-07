package com.rebel.util;

import java.lang.reflect.Method;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

public final class UrlBuilder {

    private final String baseUrl;
    
    private UrlBuilder(String baseUrl) {
		super();
		this.baseUrl = baseUrl;
	}

	/**
     * Crea un UrlBuilder leggendo il @RequestMapping
     * del controller.
     */
    public static UrlBuilder settingBaseUrl(Class<?> controllerImpl) {

        RequestMapping mapping =
                controllerImpl.getAnnotation(RequestMapping.class);

        String baseUrl = mapping != null && mapping.value().length > 0
                ? mapping.value()[0]
                : "";

        return new UrlBuilder(baseUrl);
    }

    public String buildGetUrl(
            Class<?> controllerInterface,
            String methodName,
            Class<?>... parameterTypes) throws Exception {

        checkBaseUrl();

        Method method = controllerInterface.getMethod(
                methodName,
                parameterTypes
        );

        GetMapping mapping =
                method.getAnnotation(GetMapping.class);

        if (mapping == null) {
            throw new IllegalArgumentException(
                    "Method " + methodName +
                    " is not annotated with @GetMapping"
            );
        }

        return baseUrl + mapping.value()[0];
    }

    public String buildPostUrl(
            Class<?> controllerInterface,
            String methodName,
            Class<?>... parameterTypes) throws Exception {

        checkBaseUrl();

        Method method = controllerInterface.getMethod(
                methodName,
                parameterTypes
        );

        PostMapping mapping =
                method.getAnnotation(PostMapping.class);

        if (mapping == null) {
            throw new IllegalArgumentException(
                    "Method " + methodName +
                    " is not annotated with @PostMapping"
            );
        }

        return baseUrl + mapping.value()[0];
    }

    private void checkBaseUrl() throws IllegalStateException {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Base URL is not set");
        }
    }

	public String getBaseUrl() {
		return baseUrl;
	}
    
}