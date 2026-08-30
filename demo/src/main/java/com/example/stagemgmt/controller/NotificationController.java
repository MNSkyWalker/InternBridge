package com.example.stagemgmt.controller;

import com.example.stagemgmt.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Un seul geste : masquer une notification pour l'utilisateur courant (persisté,
 *  donc ça reste masqué même après reconnexion). Pas de "marquer comme non lu" -
 *  pas demandé, pas de complexité inutile pour l'instant. */
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/notifications/masquer")
    public ResponseEntity<Void> masquer(@RequestParam String id, Authentication authentication) {
        notificationService.masquer(authentication.getName(), id);
        return ResponseEntity.ok().build();
    }
}
