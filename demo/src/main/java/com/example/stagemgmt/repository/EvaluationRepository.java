package com.example.stagemgmt.repository;

import com.example.stagemgmt.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByStagiaireIdOrderByDateDesc(Long stagiaireId);
}
