package com.rebel.api.core.auth;

public record LoginResponse(String token, Integer expiresIn) {

}