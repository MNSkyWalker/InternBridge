package com.example.stagemgmt.repository;

import com.example.stagemgmt.entity.Reunion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReunionRepository extends JpaRepository<Reunion, Long> {
    List<Reunion> findByStagiaireIdOrderByDateHeureDesc(Long stagiaireId);
    List<Reunion> findByStagiaireIdInOrderByDateHeureAsc(List<Long> stagiaireIds);
}
