package com.example.stagemgmt.repository;

import com.example.stagemgmt.entity.JournalActivite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalActiviteRepository extends JpaRepository<JournalActivite, Long> {
    List<JournalActivite> findByActeurUsernameOrderByDateHeureDesc(String username);
    List<JournalActivite> findByStagiaireIdInOrderByDateHeureDesc(List<Long> stagiaireIds);
}
