package com.example.stagemgmt.service;

import com.example.stagemgmt.entity.Evaluation;
import com.example.stagemgmt.repository.EvaluationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;

    public EvaluationService(EvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    public List<Evaluation> findByStagiaire(Long stagiaireId) {
        return evaluationRepository.findByStagiaireIdOrderByDateDesc(stagiaireId);
    }

    public Evaluation findById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Évaluation introuvable"));
    }

    @Transactional
    public Evaluation ajouter(Evaluation evaluation) {
        return evaluationRepository.save(evaluation);
    }

    @Transactional
    public void supprimer(Long id) {
        evaluationRepository.deleteById(id);
    }
}
