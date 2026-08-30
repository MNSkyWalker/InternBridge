package com.example.stagemgmt.repository;

import com.example.stagemgmt.entity.DocumentStagiaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentStagiaireRepository extends JpaRepository<DocumentStagiaire, Long> {
    List<DocumentStagiaire> findByStagiaireIdOrderByDateAjoutDesc(Long stagiaireId);
}
