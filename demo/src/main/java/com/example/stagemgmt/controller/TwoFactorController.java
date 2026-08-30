package com.example.stagemgmt.controller;

import com.example.stagemgmt.entity.Utilisateur;
import com.example.stagemgmt.repository.UtilisateurRepository;
import com.example.stagemgmt.security.RoleBasedRedirectHandler;
import com.example.stagemgmt.security.TwoFactorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Étape de connexion en 2 temps : une fois le mot de passe validé, un compte avec la
 *  2FA activée doit encore prouver qu'il possède l'appli d'authentification avant
 *  d'accéder à quoi que ce soit d'autre (voir TwoFactorGateFilter). */
@Controller
public class TwoFactorController {

    private final UtilisateurRepository utilisateurRepository;
    private final TwoFactorService twoFactorService;

    public TwoFactorController(UtilisateurRepository utilisateurRepository, TwoFactorService twoFactorService) {
        this.utilisateurRepository = utilisateurRepository;
        this.twoFactorService = twoFactorService;
    }

    @GetMapping("/2fa/verify")
    public String verifyForm() {
        return "2fa-verify";
    }

    @PostMapping("/2fa/verify")
    public String verify(@RequestParam String code, Authentication authentication,
                          HttpSession session, Model model) {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(authentication.getName())
                .orElseThrow();

        if (!twoFactorService.verifierCode(utilisateur.getTwoFactorSecret(), code)) {
            model.addAttribute("error", "Code invalide. Réessayez.");
            return "2fa-verify";
        }

        session.removeAttribute(RoleBasedRedirectHandler.SESSION_PENDING_2FA);

        boolean estResponsable = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_RESPONSABLE") || a.equals("ROLE_ENCADREUR"));
        return "redirect:" + (estResponsable ? "/responsable/dashboard" : "/stagiaire/dashboard");
    }
}
