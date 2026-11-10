package com.rebel.teach.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

// Spring rileva questa classe e applica le personalizzazioni di WebFlux.
@Configuration
// WebFluxConfigurer permette di estendere la configurazione dei controller reattivi.
public class PageableWebFluxConfiguration implements WebFluxConfigurer {

 // WebFlux chiama questo metodo per registrare i resolver degli argomenti:
 // componenti che ricavano i parametri dei metodi dei controller dalla richiesta HTTP.
 @Override
 public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
     // Converte i parametri di paginazione della query string in un oggetto Pageable.
     // Esempio: ?page=0&size=10&sort=name,asc
     // produce una richiesta per la prima pagina, con 10 elementi per pagina,
     // ordinati per nome crescente. Di default, il numero della pagina parte da 0.
     // Il resolver reattivo gestisce anche l'ordinamento tramite il supporto a Sort.
     // Nei controller basta quindi dichiarare un parametro Pageable.
     // Questa configurazione interpreta la richiesta: la query paginata viene
     // eseguita dal repository quando il controller gli passa il Pageable.
     configurer.addCustomResolver(new ReactivePageableHandlerMethodArgumentResolver());
 }

}

