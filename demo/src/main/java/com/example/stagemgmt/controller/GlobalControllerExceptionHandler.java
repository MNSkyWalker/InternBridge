package com.example.stagemgmt.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Convertit les erreurs métier en page d'erreur simple plutôt que la page blanche
 *  par défaut de Spring - suffisant pour un projet de ce niveau. */
@ControllerAdvice
public class GlobalControllerExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public String handleSecurity(SecurityException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    /** Le multipart resolver de Spring rejette les fichiers trop lourds avant même
     *  d'atteindre le contrôleur (donc son try/catch ne peut pas l'intercepter) -
     *  sans ce handler, l'utilisateur tombe sur une erreur générique illisible. */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(Model model) {
        model.addAttribute("message", "Le fichier envoyé dépasse la taille maximale autorisée (10 Mo).");
        return "error";
    }
}
