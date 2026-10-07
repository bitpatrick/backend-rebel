//package com.rebel.gateway;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//
//public class EurekaTestClient {
//
//    private final String eurekaUrl;
//    private final String username;
//    private final String password;
//
//    private final HttpClient httpClient = HttpClient.newHttpClient();
//
//    public EurekaTestClient(
//            String eurekaUrl,
//            String username,
//            String password) {
//
//        this.eurekaUrl = eurekaUrl;
//        this.username = username;
//        this.password = password;
//    }
//
//    public void register(
//            String appName,
//            String instanceId,
//            String host,
//            int port) throws Exception {
//
//        // =====================================================
//        // BODY DELLA REGISTRAZIONE EUREKA
//        // =====================================================
//
//        String body = """
//                {
//                  "instance": {
//                    "instanceId": "%s",
//                    "hostName": "%s",
//                    "app": "%s",
//                    "ipAddr": "%s",
//                    "vipAddress": "%s",
//                    "status": "UP",
//                    "port": {
//                      "$": %d,
//                      "@enabled": "true"
//                    },
//                    "dataCenterInfo": {
//                      "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
//                      "name": "MyOwn"
//                    }
//                  }
//                }
//                """.formatted(
//                    instanceId,
//                    host,
//                    appName,
//                    host,
//                    appName,
//                    port
//                );
//
//        // =====================================================
//        // BASIC AUTH
//        // =====================================================
//
//        String credentials = username + ":" + password;
//
//        String encodedCredentials = Base64.getEncoder()
//                .encodeToString(
//                        credentials.getBytes(StandardCharsets.UTF_8)
//                );
//
//        // =====================================================
//        // REQUEST
//        // =====================================================
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(
//                    URI.create(
//                        eurekaUrl + "/apps/" + appName
//                    )
//                )
//
//                // username:password
//                // viene inviato a Eureka tramite Basic Authentication
//                .header(
//                    "Authorization",
//                    "Basic " + encodedCredentials
//                )
//
//                .header(
//                    "Content-Type",
//                    "application/json"
//                )
//
//                .POST(
//                    HttpRequest.BodyPublishers.ofString(body)
//                )
//
//                .build();
//
//        // =====================================================
//        // INVIO REQUEST
//        // =====================================================
//
//        HttpResponse<String> response = httpClient.send(
//                request,
//                HttpResponse.BodyHandlers.ofString()
//        );
//
//        // =====================================================
//        // CONTROLLO RISPOSTA
//        // =====================================================
//
//        if (response.statusCode() != 204
//                && response.statusCode() != 200) {
//
//            throw new IllegalStateException(
//                    "Eureka registration failed: "
//                    + response.statusCode()
//                    + " - "
//                    + response.body()
//            );
//        }
//    }
//}