package com.example.stagemgmt.repository;

import com.example.stagemgmt.entity.Livrable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LivrableRepository extends JpaRepository<Livrable, Long> {
    List<Livrable> findByStagiaireIdOrderByDateEcheanceAsc(Long stagiaireId);
}
