package com.example.stagemgmt.controller;

import com.example.stagemgmt.dto.Notification;
import com.example.stagemgmt.repository.UtilisateurRepository;
import com.example.stagemgmt.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Attributs disponibles sur TOUTES les pages (peu importe le contrôleur), pour que
 * le fragment navbar.html ne casse jamais faute d'attribut manquant. C'est ce qui
 * causait le rendu cassé du dashboard à l'origine : ${notificationsCount > 0}
 * plantait dès que ce paramètre était absent du modèle.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;

    public GlobalModelAttributes(UtilisateurRepository utilisateurRepository, NotificationService notificationService) {
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
    }

    @ModelAttribute("userRole")
    public String userRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "Invité";
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("Utilisateur")
                .replace("ROLE_", "");
    }

    @ModelAttribute("photoProfil")
    public String photoProfil(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return utilisateurRepository.findByUsername(authentication.getName())
                .map(com.example.stagemgmt.entity.Utilisateur::getPhotoProfil)
                .orElse(null);
    }

    @ModelAttribute("nomComplet")
    public String nomComplet(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "";
        }
        return utilisateurRepository.findByUsername(authentication.getName())
                .map(com.example.stagemgmt.entity.Utilisateur::getNomComplet)
                .orElse(authentication.getName());
    }

    @ModelAttribute
    public void notifications(Authentication authentication, org.springframework.ui.Model model) {
        List<Notification> notifications = (authentication != null && authentication.isAuthenticated())
                ? notificationService.pourResponsable(authentication.getName())
                : List.of();
        model.addAttribute("notifications", notifications);
        model.addAttribute("notificationsCount", notifications.size());
    }
}
