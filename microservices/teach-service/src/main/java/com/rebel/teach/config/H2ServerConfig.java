package com.rebel.teach.config;

import java.sql.SQLException;

import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "h2-server.enabled", havingValue = "true", matchIfMissing = true)
public class H2ServerConfig {

    private Server webServer;

    @Value("${h2-server.port:8082}")
    private Integer h2ConsolePort;

    // Avvia la console H2 quando l'app è pronta
    @EventListener(ApplicationReadyEvent.class)
    public void start() throws SQLException {
    	webServer = Server.createWebServer(
    	        "-webPort", h2ConsolePort.toString(),
    	        
    	        // Abilita accesso remoto alla console
    	        "-webAllowOthers",
    	        
    	        // connessioni JDBC TCP remote
    	        "-tcpAllowOthers",
    	        
    	        // Serve per client PostgreSQL
    	        "-pgAllowOthers",
    	        
    	        // Autorizza gli hostname esterni
    	        "-webExternalNames", "localhost,teach-service"
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
