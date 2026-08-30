package com.example.stagemgmt.controller;

import com.example.stagemgmt.entity.Utilisateur;
import com.example.stagemgmt.repository.UtilisateurRepository;
import com.example.stagemgmt.security.TwoFactorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Base64;
import java.util.Set;

/** Espace "Mon profil", accessible aux deux rôles : photo, nom d'utilisateur,
 *  mot de passe, activation/désactivation de la 2FA. */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Set<String> TYPES_IMAGE_AUTORISES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long TAILLE_MAX_IMAGE = 2 * 1024 * 1024; // 2MB

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final TwoFactorService twoFactorService;

    @org.springframework.beans.factory.annotation.Value("${app.ad.enabled:false}")
    private boolean adModeActif;

    public ProfileController(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
                              TwoFactorService twoFactorService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.twoFactorService = twoFactorService;
    }

    @GetMapping
    public String monProfil(Authentication authentication, Model model) {
        Utilisateur u = utilisateurRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("utilisateur", u);
        model.addAttribute("adModeActif", adModeActif);
        return "profile";
    }

    @PostMapping("/photo")
    public String changerPhoto(@RequestParam("photo") MultipartFile photo, Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (photo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez choisir une image.");
            return "redirect:/profile";
        }
        if (photo.getSize() > TAILLE_MAX_IMAGE) {
            redirectAttributes.addFlashAttribute("error", "L'image doit faire 2 Mo maximum.");
            return "redirect:/profile";
        }
        String contentType = photo.getContentType();
        if (contentType == null || !TYPES_IMAGE_AUTORISES.contains(contentType)) {
            redirectAttributes.addFlashAttribute("error", "Formats acceptés : JPEG, PNG, WEBP, GIF.");
            return "redirect:/profile";
        }

        try {
            Utilisateur u = utilisateurRepository.findByUsername(authentication.getName()).orElseThrow();
            String base64 = Base64.getEncoder().encodeToString(photo.getBytes());
            u.setPhotoProfil("data:" + contentType + ";base64," + base64);
            utilisateurRepository.save(u);
            redirectAttributes.addFlashAttribute("success", "Photo de profil mise à jour.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Impossible de lire l'image envoyée.");
        }
        return "redirect:/profile";
    }

    @PostMapping("/photo/supprimer")
    public String supprimerPhoto(Authentication authentication, RedirectAttributes redirectAttributes) {
        Utilisateur u = utilisateurRepository.findByUsername(authentication.getName()).orElseThrow();
        u.setPhotoProfil(null);
        utilisateurRepository.save(u);
        redirectAttributes.addFlashAttribute("success", "Photo de profil supprimée.");
        return "redirect:/profile";
    }

    /** Changer de nom d'utilisateur force une reconnexion : la session courante est
     *  liée à l'ancien username, la garder active créerait des incohérences subtiles
     *  partout où on relit authentication.getName(). Plus sûr de repartir propre. */
    @PostMapping("/username")
    public String changerUsername(@RequestParam String nouveauUsername, Authentication authentication,
                                   HttpServletRequest request, HttpServletResponse response,
                                   RedirectAttributes redirectAttributes) {
        if (adModeActif) {
            redirectAttributes.addFlashAttribute("error", "Identifiants gérés par l'Active Directory de la banque.");
            return "redirect:/profile";
        }
        String actuel = authentication.getName();
        if (nouveauUsername.equals(actuel)) {
            redirectAttributes.addFlashAttribute("error", "C'est déjà votre nom d'utilisateur actuel.");
            return "redirect:/profile";
        }
        if (utilisateurRepository.existsByUsername(nouveauUsername)) {
            redirectAttributes.addFlashAttribute("error", "Ce nom d'utilisateur est déjà pris.");
            return "redirect:/profile";
        }

        Utilisateur u = utilisateurRepository.findByUsername(actuel).orElseThrow();
        u.setUsername(nouveauUsername);
        utilisateurRepository.save(u);

        new SecurityContextLogoutHandler().logout(request, response, authentication);
        SecurityContextHolder.clearContext();

        return "redirect:/login?renamed";
    }

    @PostMapping("/password")
    public String changerMotDePasse(@RequestParam String motDePasseActuel,
                                     @RequestParam String nouveauMotDePasse,
                                     Authentication authentication, RedirectAttributes redirectAttributes) {
        if (adModeActif) {
            redirectAttributes.addFlashAttribute("error", "Identifiants gérés par l'Active Directory de la banque.");
            return "redirect:/profile";
        }
        Utilisateur u = utilisateurRepository.findByUsername(authentication.getName()).orElseThrow();

        if (!passwordEncoder.matches(motDePasseActuel, u.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe actuel incorrect.");
            return "redirect:/profile";
        }
        if (nouveauMotDePasse.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Le nouveau mot de passe doit faire au moins 8 caractères.");
            return "redirect:/profile";
        }

        u.setPassword(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(u);
        redirectAttributes.addFlashAttribute("success", "Mot de passe mis à jour.");
        return "redirect:/profile";
    }

    /** Étape 1 : génère un secret + QR code, affiché directement (pas de redirection,
     *  pour ne pas avoir à faire transiter l'image ailleurs que dans cette réponse). */
    @PostMapping("/2fa/activer")
    public String activer2FA(Authentication authentication, Model model) {
        Utilisateur u = utilisateurRepository.findByUsername(authentication.getName()).orElseThrow();
        String secret = twoFactorService.genererSecret();
        u.setTwoFactorSecret(secret); // en attente de confirmation - twoFactorEnabled reste false
        utilisateurRepository.save(u);

        model.addAttribute("utilisateur", u);
        model.addAttribute("qrCode", twoFactorService.genererQrCode(u.getUsername(), secret));
        return "profile-2fa-setup";
    }

    /** Étape 2 : confirme que le code généré par l'appli correspond bien au secret. */
    @PostMapping("/2fa/confirmer")
    public String confirmer2FA(@RequestParam String code, Authentication authentication,
                                Model model, RedirectAttributes redirectAttributes) {
        Utilisateur u = utilisateurRepository.findByUsername(authentication.getName()).orElseThrow();

        if (!twoFactorService.verifierCode(u.getTwoFactorSecret(), code)) {
            model.addAttribute("utilisateur", u);
            model.addAttribute("qrCode", twoFactorService.genererQrCode(u.getUsername(), u.getTwoFactorSecret()));
            model.addAttribute("error", "Code invalide. Ouvrez votre appli d'authentification et réessayez.");
            return "profile-2fa-setup";
        }

        u.setTwoFactorEnabled(true);
        utilisateurRepository.save(u);
        redirectAttributes.addFlashAttribute("success", "Authentification à deux facteurs activée.");
        return "redirect:/profile";
    }

    @PostMapping("/2fa/desactiver")
    public String desactiver2FA(@RequestParam String motDePasseActuel, Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        Utilisateur u = utilisateurRepository.findByUsername(authentication.getName()).orElseThrow();

        if (!passwordEncoder.matches(motDePasseActuel, u.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe incorrect.");
            return "redirect:/profile";
        }

        u.setTwoFactorEnabled(false);
        u.setTwoFactorSecret(null);
        utilisateurRepository.save(u);
        redirectAttributes.addFlashAttribute("success", "Authentification à deux facteurs désactivée.");
        return "redirect:/profile";
    }
}
