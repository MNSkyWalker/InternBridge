package com.example.stagemgmt.service;

import com.example.stagemgmt.entity.Reunion;
import com.example.stagemgmt.entity.Stagiaire;
import com.example.stagemgmt.entity.StatutReunion;
import com.example.stagemgmt.repository.ReunionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ReunionService {

    private final ReunionRepository reunionRepository;
    private final StagiaireService stagiaireService;

    public ReunionService(ReunionRepository reunionRepository, StagiaireService stagiaireService) {
        this.reunionRepository = reunionRepository;
        this.stagiaireService = stagiaireService;
    }

    public List<Reunion> findByStagiaire(Long stagiaireId) {
        return reunionRepository.findByStagiaireIdOrderByDateHeureDesc(stagiaireId);
    }

    /** Toutes les réunions planifiées pour les stagiaires de ce responsable, triées par date. */
    public List<Reunion> pourResponsable(String username) {
        List<Long> ids = stagiaireService.findByResponsable(username).stream().map(Stagiaire::getId).toList();
        if (ids.isEmpty()) return List.of();
        return reunionRepository.findByStagiaireIdInOrderByDateHeureAsc(ids).stream()
                .sorted(Comparator.comparing(Reunion::getDateHeure))
                .toList();
    }

    public List<Reunion> aVenirPourResponsable(String username) {
        return pourResponsable(username).stream()
                .filter(r -> r.getStatut() == StatutReunion.PLANIFIEE && r.getDateHeure().isAfter(LocalDateTime.now().minusHours(1)))
                .toList();
    }

    public Reunion findById(Long id) {
        return reunionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Réunion introuvable"));
    }

    @Transactional
    public Reunion planifier(Reunion reunion) {
        return reunionRepository.save(reunion);
    }

    @Transactional
    public void changerStatut(Long id, StatutReunion statut) {
        Reunion r = findById(id);
        r.setStatut(statut);
        reunionRepository.save(r);
    }

    @Transactional
    public void supprimer(Long id) {
        reunionRepository.deleteById(id);
    }
}
