package com.example.stagemgmt.service;

import com.example.stagemgmt.entity.JournalActivite;
import com.example.stagemgmt.entity.Stagiaire;
import com.example.stagemgmt.entity.Utilisateur;
import com.example.stagemgmt.repository.JournalActiviteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Journal d'activité (audit trail) : qui a fait quoi et quand, pour les stagiaires
 *  gérés par un responsable donné (utile s'il y a plusieurs responsables dans l'appli). */
@Service
public class JournalActiviteService {

    private final JournalActiviteRepository journalRepository;
    private final StagiaireService stagiaireService;

    public JournalActiviteService(JournalActiviteRepository journalRepository, StagiaireService stagiaireService) {
        this.journalRepository = journalRepository;
        this.stagiaireService = stagiaireService;
    }

    @Transactional
    public void enregistrer(Utilisateur acteur, Stagiaire stagiaire, String description) {
        journalRepository.save(JournalActivite.builder()
                .dateHeure(LocalDateTime.now())
                .acteur(acteur)
                .stagiaire(stagiaire)
                .description(description)
                .build());
    }

    public List<JournalActivite> pourResponsable(String username) {
        Map<Long, JournalActivite> parId = new LinkedHashMap<>();
        journalRepository.findByActeurUsernameOrderByDateHeureDesc(username).forEach(j -> parId.put(j.getId(), j));

        List<Long> stagiaireIds = stagiaireService.findByResponsable(username).stream().map(Stagiaire::getId).toList();
        if (!stagiaireIds.isEmpty()) {
            journalRepository.findByStagiaireIdInOrderByDateHeureDesc(stagiaireIds).forEach(j -> parId.put(j.getId(), j));
        }

        return parId.values().stream()
                .sorted(Comparator.comparing(JournalActivite::getDateHeure).reversed())
                .limit(100)
                .toList();
    }
}
