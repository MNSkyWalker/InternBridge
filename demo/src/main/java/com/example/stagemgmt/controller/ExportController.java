package com.example.stagemgmt.controller;

import com.example.stagemgmt.entity.Stagiaire;
import com.example.stagemgmt.service.EvaluationService;
import com.example.stagemgmt.service.LivrableService;
import com.example.stagemgmt.service.StagiaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Export CSV de la liste des stagiaires, et rapport imprimable par stagiaire
 *  (page HTML avec CSS d'impression - "Imprimer > Enregistrer en PDF" dans le
 *  navigateur donne un PDF propre sans dépendance supplémentaire). */
@Controller
@RequestMapping("/responsable")
@RequiredArgsConstructor
public class ExportController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StagiaireService stagiaireService;
    private final LivrableService livrableService;
    private final EvaluationService evaluationService;

    @GetMapping("/export/stagiaires.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exporterCsv(Authentication authentication) {
        List<Stagiaire> stagiaires = stagiaireService.findByResponsable(authentication.getName());

        StringBuilder csv = new StringBuilder();
        csv.append("Nom;École;Année;Filière;Cycle;Sujet;Début;Fin;Avancement (%)\n");
        for (Stagiaire s : stagiaires) {
            int avancement = livrableService.avancementMoyen(s.getId());
            csv.append(csv(s.getNomComplet())).append(';')
               .append(csv(s.getEcole())).append(';')
               .append(s.getAnneeInscription() == null ? "" : s.getAnneeInscription()).append(';')
               .append(csv(s.getFiliere())).append(';')
               .append(s.getCycle() == null ? "" : s.getCycle()).append(';')
               .append(csv(s.getTitreSujet())).append(';')
               .append(s.getDateDebut() == null ? "" : s.getDateDebut().format(DATE)).append(';')
               .append(s.getDateFin() == null ? "" : s.getDateFin().format(DATE)).append(';')
               .append(avancement).append('\n');
        }

        byte[] contenu = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("stagiaires.csv").build().toString())
                .body(contenu);
    }

    private String csv(String valeur) {
        if (valeur == null) return "";
        return valeur.replace(";", ",").replace("\n", " ");
    }

    @GetMapping("/stagiaires/{id}/rapport")
    public String rapport(@PathVariable Long id, Authentication authentication, Model model) {
        Stagiaire stagiaire = stagiaireService.findById(id);
        stagiaireService.verifierProprietaire(stagiaire, authentication.getName());

        model.addAttribute("stagiaire", stagiaire);
        model.addAttribute("livrables", livrableService.findByStagiaire(id));
        model.addAttribute("evaluations", evaluationService.findByStagiaire(id));
        model.addAttribute("avancementMoyen", livrableService.avancementMoyen(id));
        return "responsable/rapport";
    }
}
