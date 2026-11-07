package it.cvmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * BFF con sessione server-side:
 * - login: la SPA invia email/password come form-urlencoded a /api/auth/login (204 o 401)
 * - sessione in PostgreSQL (Spring Session JDBC), cookie HttpOnly
 * - CSRF con cookie XSRF-TOKEN + header X-XSRF-TOKEN
 * - le API rispondono 401 invece di fare redirect a una pagina di login
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/**", "/actuator/**").authenticated()
                        // la SPA (index.html, asset, rotte client-side) è pubblica: i dati passano solo da /api
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.spa())
                // nessun redirect post-login lato server: evita di creare sessioni per ogni 401 anonimo
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form
                        // pagina della SPA: evita che Spring Security generi la sua pagina di login
                        .loginPage("/login")
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((request, response, exception) ->
                                response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**")));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // bcrypt di default, con prefisso {id} per poter migrare algoritmo in futuro
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
