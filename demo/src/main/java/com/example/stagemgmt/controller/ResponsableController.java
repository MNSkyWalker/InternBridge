package com.example.stagemgmt.controller;

import com.example.stagemgmt.entity.*;
import com.example.stagemgmt.repository.UtilisateurRepository;
import com.example.stagemgmt.service.EvaluationService;
import com.example.stagemgmt.service.JournalActiviteService;
import com.example.stagemgmt.service.LivrableService;
import com.example.stagemgmt.service.StagiaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Deux rôles se partagent cet espace : le responsable gère lui-même la fiche de
 *  chaque stagiaire (identité + sujet + période, en une seule fois) et saisit
 *  l'avancement des livrables ; l'encadreur (superviseur bancaire) a accès aux
 *  mêmes fonctions pour les stagiaires qui lui sont assignés, à l'exception de la
 *  création de nouvelles fiches stagiaire et de la création de comptes encadreur,
 *  réservées au responsable (voir SecurityConfig). */
@Controller
@RequestMapping("/responsable")
@RequiredArgsConstructor
@Slf4j
public class ResponsableController {

    private final StagiaireService stagiaireService;
    private final LivrableService livrableService;
    private final EvaluationService evaluationService;
    private final UtilisateurRepository utilisateurRepository;
    private final JournalActiviteService journalActiviteService;
    private final com.example.stagemgmt.service.DocumentService documentService;
    private final com.example.stagemgmt.service.ReunionService reunionService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        List<Stagiaire> stagiaires = stagiaireService.findByResponsable(authentication.getName());

        Map<Long, Integer> avancementParStagiaire = new HashMap<>();
        Map<Long, StatutLivrable> statutParStagiaire = new HashMap<>();
        int totalProgress = 0;
        int enCours = 0;
        int termines = 0;

        for (Stagiaire s : stagiaires) {
            int progress = livrableService.avancementMoyen(s.getId());
            StatutLivrable statutGlobal = livrableService.statutGlobal(s.getId());
            avancementParStagiaire.put(s.getId(), progress);
            statutParStagiaire.put(s.getId(), statutGlobal);
            totalProgress += progress;
            // Même critère que le badge affiché sur chaque carte (statutGlobal), pour que
            // le compteur "Stages terminés" soit toujours cohérent avec ce que l'utilisateur voit.
            if (statutGlobal == StatutLivrable.TERMINE) termines++; else enCours++;
        }

        int avancementMoyenGlobal = stagiaires.isEmpty() ? 0 : totalProgress / stagiaires.size();

        model.addAttribute("stagiaires", stagiaires);
        model.addAttribute("avancementParStagiaire", avancementParStagiaire);
        model.addAttribute("statutParStagiaire", statutParStagiaire);
        model.addAttribute("avancementMoyenGlobal", avancementMoyenGlobal);
        model.addAttribute("stagiairesEnCours", enCours);
        model.addAttribute("stagiairesTermines", termines);
        return "responsable/dashboard";
    }

    @GetMapping("/journal")
    public String journal(Authentication authentication, Model model) {
        model.addAttribute("entrees", journalActiviteService.pourResponsable(authentication.getName()));
        return "journal";
    }

    /** Réservé au responsable (voir SecurityConfig) : l'encadreur ne peut pas créer de fiche stagiaire. */
    @GetMapping("/stagiaires/nouveau")
    public String nouveauStagiaireForm() {
        return "responsable/nouveau-stagiaire";
    }

    /** Un seul formulaire crée toute la fiche : identité de l'étudiant + sujet de stage.
     *  Réservé au responsable (voir SecurityConfig). */
    @PostMapping("/stagiaires")
    public String creerStagiaire(@RequestParam String nomComplet,
                                  @RequestParam(required = false) String email,
                                  @RequestParam(required = false) String ecole,
                                  @RequestParam(required = false) Integer anneeInscription,
                                  @RequestParam(required = false) String filiere,
                                  @RequestParam(required = false) CycleEtudes cycle,
                                  @RequestParam String titreSujet,
                                  @RequestParam(required = false) String descriptionSujet,
                                  @RequestParam LocalDate dateDebut,
                                  @RequestParam LocalDate dateFin,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {

        Utilisateur responsable = utilisateurRepository.findByUsername(authentication.getName()).orElseThrow();

        Stagiaire stagiaire = Stagiaire.builder()
                .nomComplet(nomComplet)
                .email(email)
                .ecole(ecole)
                .anneeInscription(anneeInscription)
                .filiere(filiere)
                .cycle(cycle)
                .titreSujet(titreSujet)
                .descriptionSujet(descriptionSujet)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .responsable(responsable)
                .build();
        stagiaireService.creer(stagiaire);

        journalActiviteService.enregistrer(responsable, stagiaire, "A ajouté la fiche de " + nomComplet);
        log.info("Nouvelle fiche stagiaire créée : {}", nomComplet);

        redirectAttributes.addFlashAttribute("success", "Fiche stagiaire créée avec succès !");
        return "redirect:/responsable/dashboard";
    }

    @GetMapping("/stagiaires/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        Stagiaire stagiaire = stagiaireService.findById(id);
        stagiaireService.verifierProprietaire(stagiaire, authentication.getName());

        model.addAttribute("stagiaire", stagiaire);
        model.addAttribute("livrables", livrableService.findByStagiaire(id));
        model.addAttribute("evaluations", evaluationService.findByStagiaire(id));
        model.addAttribute("avancementMoyen", livrableService.avancementMoyen(id));
        model.addAttribute("statuts", StatutLivrable.values());
        model.addAttribute("documents", documentService.findByStagiaire(id));
        model.addAttribute("reunionsStagiaire", reunionService.findByStagiaire(id));
        model.addAttribute("encadreurs", utilisateurRepository.findByRoleOrderByNomCompletAsc(Role.ENCADREUR));
        return "responsable/stagiaire-detail";
    }

    /** Assigne un encadreur (compte existant avec le rôle ENCADREUR) à ce stagiaire. */
    @PostMapping("/stagiaires/{id}/encadreur")
    public String assignerEncadreur(@PathVariable Long id,
                                     @RequestParam(required = false) String encadreurId,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        Stagiaire stagiaire = stagiaireService.findById(id);
        stagiaireService.verifierProprietaire(stagiaire, authentication.getName());

        if (encadreurId == null || encadreurId.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Choisissez un encadreur dans la liste.");
            return "redirect:/responsable/stagiaires/" + id;
        }

        Utilisateur encadreur = utilisateurRepository.findById(Long.valueOf(encadreurId))
                .filter(u -> u.getRole() == Role.ENCADREUR)
                .orElseThrow(() -> new IllegalArgumentException("Encadreur introuvable"));

        stagiaire.setEncadreur(encadreur);
        stagiaireService.enregistrer(stagiaire);

        Utilisateur acteur = utilisateurRepository.findByUsername(authentication.getName()).orElse(stagiaire.getResponsable());
        journalActiviteService.enregistrer(acteur, stagiaire, "A assigné " + encadreur.getNomComplet() + " comme encadreur");

        redirectAttributes.addFlashAttribute("success", "Encadreur assigné avec succès !");
        return "redirect:/responsable/stagiaires/" + id;
    }

    /** Liste des comptes encadreur + création. Réservé au responsable (voir SecurityConfig) :
     *  la création de comptes est une action administrative. */
    @GetMapping("/encadreurs")
    public String encadreurs(Model model) {
        model.addAttribute("encadreurs", utilisateurRepository.findByRoleOrderByNomCompletAsc(Role.ENCADREUR));
        return "responsable/encadreurs";
    }

    @PostMapping("/encadreurs")
    public String creerEncadreur(@RequestParam String username,
                                  @RequestParam String nomComplet,
                                  @RequestParam String email,
                                  @RequestParam String password,
                                  RedirectAttributes redirectAttributes) {
        if (utilisateurRepository.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "Ce nom d'utilisateur est déjà pris.");
            return "redirect:/responsable/encadreurs";
        }
        if (utilisateurRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Cet email est déjà utilisé.");
            return "redirect:/responsable/encadreurs";
        }

        utilisateurRepository.save(Utilisateur.builder()
                .username(username)
                .nomComplet(nomComplet)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ENCADREUR)
                .actif(true)
                .build());

        redirectAttributes.addFlashAttribute("success", "Compte encadreur créé avec succès !");
        return "redirect:/responsable/encadreurs";
    }

    @PostMapping("/stagiaires/{id}/livrables")
    public String ajouterLivrable(@PathVariable Long id,
                                   @RequestParam String titre,
                                   @RequestParam String description,
                                   @RequestParam LocalDate dateEcheance,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        Stagiaire stagiaire = stagiaireService.findById(id);
        stagiaireService.verifierProprietaire(stagiaire, authentication.getName());

        Livrable livrable = Livrable.builder()
                .titre(titre)
                .description(description)
                .dateEcheance(dateEcheance)
                .statut(StatutLivrable.CADRAGE)
                .avancement(0)
                .stagiaire(stagiaire)
                .build();

        livrableService.ajouter(livrable);
        Utilisateur acteur = utilisateurRepository.findByUsername(authentication.getName()).orElse(stagiaire.getResponsable());
        journalActiviteService.enregistrer(acteur, stagiaire, "A ajouté la tâche \"" + titre + "\"");

        redirectAttributes.addFlashAttribute("success", "Tâche ajoutée avec succès !");
        return "redirect:/responsable/stagiaires/" + id;
    }

    /** Le responsable (ou l'encadreur assigné) saisit lui-même l'avancement (plus d'auto-déclaration). */
    @PostMapping("/stagiaires/{id}/livrables/{livrableId}")
    public String mettreAJourLivrable(@PathVariable Long id, @PathVariable Long livrableId,
                                       @RequestParam int avancement,
                                       @RequestParam StatutLivrable statut,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        Livrable livrable = livrableService.mettreAJourAvancement(livrableId, avancement, statut, authentication.getName());
        Utilisateur acteur = utilisateurRepository.findByUsername(authentication.getName()).orElse(livrable.getStagiaire().getResponsable());
        journalActiviteService.enregistrer(acteur, livrable.getStagiaire(),
                "A mis à jour \"" + livrable.getTitre() + "\" (" + avancement + "%, " + statut + ")");

        redirectAttributes.addFlashAttribute("success", "Avancement mis à jour.");
        return "redirect:/responsable/stagiaires/" + id;
    }

    @PostMapping("/stagiaires/{id}/evaluations")
    public String ajouterEvaluation(@PathVariable Long id,
                                     @RequestParam Integer note,
                                     @RequestParam String commentaire,
                                     @RequestParam(defaultValue = "false") boolean finale,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        Stagiaire stagiaire = stagiaireService.findById(id);
        stagiaireService.verifierProprietaire(stagiaire, authentication.getName());

        Evaluation evaluation = Evaluation.builder()
                .stagiaire(stagiaire)
                .date(LocalDate.now())
                .note(note)
                .commentaire(commentaire)
                .finale(finale)
                .build();

        evaluationService.ajouter(evaluation);
        Utilisateur acteur = utilisateurRepository.findByUsername(authentication.getName()).orElse(stagiaire.getResponsable());
        journalActiviteService.enregistrer(acteur, stagiaire,
                "A ajouté une évaluation" + (finale ? " finale" : " intermédiaire") + " (" + note + "/20)");

        redirectAttributes.addFlashAttribute("success", "Évaluation ajoutée avec succès !");
        return "redirect:/responsable/stagiaires/" + id;
    }

    @PostMapping("/stagiaires/{id}/livrables/{livrableId}/supprimer")
    public String supprimerLivrable(@PathVariable Long id, @PathVariable Long livrableId,
                                     Authentication authentication, RedirectAttributes redirectAttributes) {
        Livrable livrable = livrableService.findById(livrableId);
        stagiaireService.verifierProprietaire(livrable.getStagiaire(), authentication.getName());

        livrableService.supprimer(livrableId);
        Utilisateur acteur = utilisateurRepository.findByUsername(authentication.getName()).orElse(livrable.getStagiaire().getResponsable());
        journalActiviteService.enregistrer(acteur, livrable.getStagiaire(), "A supprimé la tâche \"" + livrable.getTitre() + "\"");

        redirectAttributes.addFlashAttribute("success", "Tâche supprimée.");
        return "redirect:/responsable/stagiaires/" + id;
    }

    @PostMapping("/stagiaires/{id}/evaluations/{evaluationId}/supprimer")
    public String supprimerEvaluation(@PathVariable Long id, @PathVariable Long evaluationId,
                                       Authentication authentication, RedirectAttributes redirectAttributes) {
        Evaluation evaluation = evaluationService.findById(evaluationId);
        stagiaireService.verifierProprietaire(evaluation.getStagiaire(), authentication.getName());

        evaluationService.supprimer(evaluationId);
        Utilisateur acteur = utilisateurRepository.findByUsername(authentication.getName()).orElse(evaluation.getStagiaire().getResponsable());
        journalActiviteService.enregistrer(acteur, evaluation.getStagiaire(), "A supprimé une évaluation (" + evaluation.getNote() + "/20)");

        redirectAttributes.addFlashAttribute("success", "Évaluation supprimée.");
        return "redirect:/responsable/stagiaires/" + id;
    }
}
