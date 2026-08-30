package com.example.stagemgmt.controller;

import com.example.stagemgmt.entity.Reunion;
import com.example.stagemgmt.entity.Stagiaire;
import com.example.stagemgmt.entity.StatutReunion;
import com.example.stagemgmt.entity.Utilisateur;
import com.example.stagemgmt.repository.UtilisateurRepository;
import com.example.stagemgmt.service.JournalActiviteService;
import com.example.stagemgmt.service.ReunionService;
import com.example.stagemgmt.service.StagiaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/responsable/reunions")
@RequiredArgsConstructor
public class ReunionController {

    private final ReunionService reunionService;
    private final StagiaireService stagiaireService;
    private final JournalActiviteService journalActiviteService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public String liste(Authentication authentication, Model model) {
        List<Reunion> toutes = reunionService.pourResponsable(authentication.getName());
        model.addAttribute("reunions", toutes);
        model.addAttribute("stagiaires", stagiaireService.findByResponsable(authentication.getName()));
        return "responsable/reunions";
    }

    @PostMapping
    public String planifier(@RequestParam Long stagiaireId,
                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime dateHeure,
                             @RequestParam(required = false) String lieu,
                             @RequestParam(required = false) String notes,
                             @RequestParam(required = false) String retour,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        Stagiaire stagiaire = stagiaireService.findById(stagiaireId);
        stagiaireService.verifierProprietaire(stagiaire, authentication.getName());

        Reunion reunion = Reunion.builder()
                .dateHeure(dateHeure)
                .lieu(lieu)
                .notes(notes)
                .statut(StatutReunion.PLANIFIEE)
                .stagiaire(stagiaire)
                .build();
        reunionService.planifier(reunion);

        Utilisateur acteur = utilisateurRepository.findByUsername(authentication.getName()).orElse(stagiaire.getResponsable());
        journalActiviteService.enregistrer(acteur, stagiaire,
                "A planifié une réunion le " + dateHeure.toLocalDate());
        redirectAttributes.addFlashAttribute("success", "Réunion planifiée !");
        // Depuis la fiche stagiaire, la modale envoie "retour" pour revenir sur place
        // plutôt que d'atterrir sur la liste globale des réunions.
        return "redirect:" + (retour != null && !retour.isBlank() ? retour : "/responsable/reunions");
    }

    @PostMapping("/{id}/statut")
    public String changerStatut(@PathVariable Long id, @RequestParam StatutReunion statut,
                                 Authentication authentication, RedirectAttributes redirectAttributes) {
        Reunion reunion = reunionService.findById(id);
        stagiaireService.verifierProprietaire(reunion.getStagiaire(), authentication.getName());

        reunionService.changerStatut(id, statut);
        redirectAttributes.addFlashAttribute("success", "Statut mis à jour.");
        return "redirect:/responsable/reunions";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id,
                             @RequestParam(required = false) String retour,
                             Authentication authentication, RedirectAttributes redirectAttributes) {
        Reunion reunion = reunionService.findById(id);
        stagiaireService.verifierProprietaire(reunion.getStagiaire(), authentication.getName());

        reunionService.supprimer(id);
        Utilisateur acteur = utilisateurRepository.findByUsername(authentication.getName()).orElse(reunion.getStagiaire().getResponsable());
        journalActiviteService.enregistrer(acteur, reunion.getStagiaire(),
                "A supprimé la réunion du " + reunion.getDateHeure().toLocalDate());

        redirectAttributes.addFlashAttribute("success", "Réunion supprimée.");
        return "redirect:" + (retour != null && !retour.isBlank() ? retour : "/responsable/reunions");
    }
}
