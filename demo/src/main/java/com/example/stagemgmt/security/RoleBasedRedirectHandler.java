package com.example.stagemgmt.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Après le mot de passe validé : si la 2FA est activée pour ce compte, on marque la
 * session comme "en attente de code" et on renvoie vers /2fa/verify au lieu du
 * tableau de bord - c'est TwoFactorGateFilter qui empêche toute autre navigation
 * tant que ce drapeau est présent en session. Sinon, direction le tableau de bord
 * (un seul type de compte existe désormais : responsable).
 *
 * Instancié directement dans SecurityConfig (pas de @Component, pour éviter d'avoir
 * deux instances qui prêtent à confusion).
 */
public class RoleBasedRedirectHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String SESSION_PENDING_2FA = "PENDING_2FA";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof UtilisateurPrincipal principal && principal.isTwoFactorEnabled()) {
            request.getSession().setAttribute(SESSION_PENDING_2FA, true);
            getRedirectStrategy().sendRedirect(request, response, "/2fa/verify");
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, "/responsable/dashboard");
    }
}
