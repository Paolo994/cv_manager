package it.cvmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Cookie di sessione dichiarato esplicitamente: altrimenti Spring Session copia gli attributi
 * dalla configurazione del servlet container e le garanzie di sicurezza dipendono dall'ambiente.
 */
@Configuration
class SessionConfig {

    @Bean
    CookieSerializer cookieSerializer(@Value("${app.session.cookie-secure:false}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(secure);
        return serializer;
    }
}
