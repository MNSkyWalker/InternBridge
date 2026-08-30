package com.example.stagemgmt.repository;

import com.example.stagemgmt.entity.Stagiaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StagiaireRepository extends JpaRepository<Stagiaire, Long> {
    List<Stagiaire> findByResponsableUsernameOrderByNomCompletAsc(String username);
    List<Stagiaire> findByEncadreurUsernameOrderByNomCompletAsc(String username);
}
