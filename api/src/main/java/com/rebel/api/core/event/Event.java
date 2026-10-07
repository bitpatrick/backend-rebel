package com.rebel.api.core.event;

import java.time.ZonedDateTime;

public record Event<K, T>(Type type, K key, T data, ZonedDateTime eventCreatedAt) {

}
