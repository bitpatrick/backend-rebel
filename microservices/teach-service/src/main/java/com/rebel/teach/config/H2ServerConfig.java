package com.rebel.teach.config;

import java.sql.SQLException;

import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class H2ServerConfig {

    private Server webServer;

    @Value("${h2-server.port:8099}")
    private Integer h2ConsolePort;

    // Avvia la console H2 quando l'app è pronta
    @EventListener(ApplicationReadyEvent.class)
    public void start() throws SQLException {
        webServer = Server.createWebServer(
                "-webPort", h2ConsolePort.toString(),
                "-webAllowOthers"
        ).start();

        System.out.println("H2 Console available at: " + webServer.getURL());
    }

    // Ferma la console H2 allo shutdown
    @EventListener(ContextClosedEvent.class)
    public void stop() {
        if (webServer != null) {
            webServer.stop();
        }
    }
}