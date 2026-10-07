# Guida ai Test del Gateway Service

## Panoramica

Il Gateway Service funge da punto di ingresso per tutte le richieste HTTP verso i microservizi. I test verificano che le richieste vengono correttamente inoltrate ai servizi appropriati in base al path.

## Configurazione del Routing

Nel file `GatewayServiceApplication.java`, il gateway è configurato con le seguenti route:

```
/api/auth/**  →  auth-service (http://localhost:8081)
/api/users/** →  auth-service (http://localhost:8081)
/api/teachers/** → teach-service (http://localhost:8082)
```

## Come Funzionano i Test

I test utilizzano **WireMock** per mockare i servizi backend:

1. **Setup**: Prima di ogni test, vengono avviati due server WireMock
   - authServiceMock sulla porta 8081
   - teachServiceMock sulla porta 8082

2. **Stubbing**: Ogni test configura il mock per rispondere a richieste specifiche

3. **Testing**: Effettua una richiesta al gateway e verifica:
   - Che la risposta sia corretta
   - Che il mock del servizio sia stato chiamato

4. **Cleanup**: Dopo ogni test, i server mock vengono fermati

## Test Disponibili

### 1. testAuthServiceRouting()
Verifica che richieste a `/api/auth/**` vengano inoltrate al auth-service

```
GET /api/auth/health (via gateway) 
  → GET /api/auth/health (al auth-service)
```

### 2. testUsersServiceRouting()
Verifica che richieste a `/api/users/**` vengono inoltrate al auth-service

```
GET /api/users/1 (via gateway)
  → GET /api/users/1 (al auth-service)
```

### 3. testTeachersServiceRouting()
Verifica che richieste a `/api/teachers/**` vengono inoltrate al teach-service

```
GET /api/teachers/1 (via gateway)
  → GET /api/teachers/1 (al teach-service)
```

## Esecuzione dei Test

```bash
# Eseguire tutti i test del gateway
mvn test

# Eseguire solo i test del gateway service
mvn test -Dtest=GatewayServiceApplicationTests
```

## Come Aggiungere Nuovi Test

Se aggiungi nuove route nel gateway, segui questo pattern:

```java
@Test
void testNewServiceRouting() {
    // 1. Configura il mock
    mockService.stubFor(
        get(urlEqualTo("/api/new-path/**"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("{...}")));

    // 2. Effettua richiesta al gateway
    webTestClient.get()
        .uri("/api/new-path/**")
        .exchange()
        .expectStatus().isOk();

    // 3. Verifica che il mock sia stato chiamato
    mockService.verify(getRequestedFor(urlEqualTo("/api/new-path/**")));
}
```

## Dipendenze Necessarie

Il test richiede:
- `spring-boot-starter-test` (già presente)
- `reactor-test` (già presente)
- `wiremock-jre8` (aggiunto nel pom.xml)

## Risoluzione dei Problemi

**Il test fallisce perché il gateway non raggiunge i mock?**
- Verifica che i mock server siano sulla porta corretta (8081 per auth, 8082 per teach)
- Verifica la configurazione del gateway in `GatewayServiceApplication.java`

**Il test timeout?**
- Aumenta il timeout di WebTestClient nel setUp()
- Verifica che i mock server siano correttamente avviati

**Errore di porta in uso?**
- Assicurati che nessun altro processo stia usando le porte 8081 e 8082
- Aumenta il numero di porta nel setUp() se necessario
