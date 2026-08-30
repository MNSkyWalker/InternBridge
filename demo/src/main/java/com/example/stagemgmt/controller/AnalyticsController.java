package com.example.stagemgmt.controller;

import com.example.stagemgmt.entity.Evaluation;
import com.example.stagemgmt.entity.Stagiaire;
import com.example.stagemgmt.entity.StatutLivrable;
import com.example.stagemgmt.service.EvaluationService;
import com.example.stagemgmt.service.LivrableService;
import com.example.stagemgmt.service.StagiaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

/** Tableau de bord analytique : répartition des stagiaires par étape, comparaison des
 *  avancements, moyenne des évaluations - construit à partir des données déjà en base,
 *  affiché avec Chart.js (CDN, pas de nouvelle dépendance backend). */
@Controller
@RequestMapping("/responsable/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final StagiaireService stagiaireService;
    private final LivrableService livrableService;
    private final EvaluationService evaluationService;

    @GetMapping
    public String analytics(Authentication authentication, Model model) {
        List<Stagiaire> stagiaires = stagiaireService.findByResponsable(authentication.getName());

        Map<StatutLivrable, Integer> repartitionStatut = new LinkedHashMap<>();
        for (StatutLivrable s : StatutLivrable.values()) repartitionStatut.put(s, 0);

        Map<String, Integer> repartitionCycle = new LinkedHashMap<>();
        List<String> noms = new ArrayList<>();
        List<Integer> avancements = new ArrayList<>();
        List<Integer> toutesLesNotes = new ArrayList<>();

        for (Stagiaire s : stagiaires) {
            StatutLivrable statut = livrableService.statutGlobal(s.getId());
            repartitionStatut.merge(statut, 1, Integer::sum);

            String cycle = s.getCycle() == null ? "Non précisé" : s.getCycle().name();
            repartitionCycle.merge(cycle, 1, Integer::sum);

            noms.add(s.getNomComplet());
            avancements.add(livrableService.avancementMoyen(s.getId()));

            for (Evaluation e : evaluationService.findByStagiaire(s.getId())) {
                if (e.getNote() != null) toutesLesNotes.add(e.getNote());
            }
        }

        double moyenneNotes = toutesLesNotes.isEmpty() ? 0
                : toutesLesNotes.stream().mapToInt(Integer::intValue).average().orElse(0);

        model.addAttribute("stagiaires", stagiaires);
        model.addAttribute("repartitionStatut", repartitionStatut);
        model.addAttribute("repartitionCycle", repartitionCycle);
        model.addAttribute("noms", noms);
        model.addAttribute("avancements", avancements);
        model.addAttribute("moyenneNotes", Math.round(moyenneNotes * 10.0) / 10.0);
        model.addAttribute("nombreEvaluations", toutesLesNotes.size());
        return "responsable/analytics";
    }
}
