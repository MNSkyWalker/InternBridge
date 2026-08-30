package com.example.stagemgmt.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tant que la session porte le drapeau PENDING_2FA (posé juste après le mot de passe
 * validé, si le compte a la 2FA activée), toute requête vers une page protégée est
 * redirigée vers /2fa/verify. Le drapeau n'est retiré qu'une fois le code TOTP validé
 * (voir TwoFactorController).
 */
public class TwoFactorGateFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        boolean pending = session != null && Boolean.TRUE.equals(session.getAttribute(RoleBasedRedirectHandler.SESSION_PENDING_2FA));

        String path = request.getRequestURI();
        boolean isAllowedWhilePending = path.equals("/2fa/verify") || path.equals("/logout")
                || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") || path.equals("/favicon.ico");

        if (pending && !isAllowedWhilePending) {
            response.sendRedirect("/2fa/verify");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
