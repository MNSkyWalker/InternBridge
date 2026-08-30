package com.example.stagemgmt.security;

import com.example.stagemgmt.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Un seul UserDetailsService (CustomUserDetailsService, adossé à la base de données)
 * est utilisé pour l'authentification par défaut - Spring Security le détecte
 * automatiquement puisque c'est le seul bean UserDetailsService présent dans le
 * contexte. Si app.ad.enabled=true, le bean ActiveDirectoryLdapAuthenticationProvider
 * ci-dessous devient le SEUL AuthenticationProvider détecté (Spring Security arrête
 * alors d'utiliser le UserDetailsService local pour l'authentification) : dans ce mode,
 * seul l'Active Directory de la banque peut authentifier, plus les mots de passe locaux.
 *
 * CSRF reste ACTIVÉ (comportement par défaut de Spring Security) : c'est ce qui permet
 * à Thymeleaf d'injecter automatiquement le jeton CSRF dans chaque <form>, y compris
 * le formulaire de déconnexion. Le désactiver casse ces formulaires (${_csrf} devient
 * introuvable dans les templates), donc on n'y touche pas.
 *
 * TwoFactorGateFilter ajoute une étape de connexion en 2 temps pour les comptes avec
 * la 2FA activée (voir RoleBasedRedirectHandler + TwoFactorController) - fonctionne
 * pareil, que l'authentification vienne de l'AD ou d'un compte local.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new RoleBasedRedirectHandler();
    }

    @Bean
    @ConditionalOnProperty(name = "app.ad.enabled", havingValue = "true")
    public ActiveDirectoryUserDetailsMapper activeDirectoryUserDetailsMapper(
            UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        return new ActiveDirectoryUserDetailsMapper(utilisateurRepository, passwordEncoder);
    }

    @Bean
    @ConditionalOnProperty(name = "app.ad.enabled", havingValue = "true")
    public ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider(
            @Value("${app.ad.domain}") String domaine,
            @Value("${app.ad.url}") String url,
            ActiveDirectoryUserDetailsMapper mapper) {
        ActiveDirectoryLdapAuthenticationProvider provider = new ActiveDirectoryLdapAuthenticationProvider(domaine, url);
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(true);
        provider.setUserDetailsContextMapper(mapper);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/login", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico",
                            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
                    ).permitAll()
                    // Actions réservées au responsable : création de fiches stagiaire et gestion
                    // des comptes encadreur (administratif). Ces règles précises doivent être
                    // évaluées AVANT la règle générale "/responsable/**" ci-dessous.
                    .requestMatchers("/responsable/stagiaires/nouveau", "/responsable/encadreurs", "/responsable/encadreurs/**")
                        .hasRole("RESPONSABLE")
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/responsable/stagiaires")
                        .hasRole("RESPONSABLE")
                    // Le reste de l'espace /responsable est partagé : le responsable ET
                    // l'encadreur y gèrent les stagiaires qui leur sont assignés.
                    .requestMatchers("/responsable/**").hasAnyRole("RESPONSABLE", "ENCADREUR")
                    .anyRequest().authenticated()
            )
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .successHandler(authenticationSuccessHandler())
                    .failureUrl("/login?error")
                    .permitAll()
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .clearAuthentication(true)
                    .permitAll()
            )
            .addFilterAfter(new TwoFactorGateFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
