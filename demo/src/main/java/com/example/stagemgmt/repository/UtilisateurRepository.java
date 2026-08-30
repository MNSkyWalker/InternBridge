package com.example.stagemgmt.repository;

import com.example.stagemgmt.entity.Role;
import com.example.stagemgmt.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<Utilisateur> findByRole(Role role);
    List<Utilisateur> findByRoleOrderByNomCompletAsc(Role role);
}
