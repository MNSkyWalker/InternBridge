package com.example.stagemgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Une entrée du journal d'activité (audit trail) : qui a fait quoi, quand, et à
 *  propos de quel stagiaire (nullable - ex: création de fiche n'est pas encore liée
 *  tant que le stagiaire n'existe pas). */
@Entity
@Table(name = "journal_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @ManyToOne(optional = false)
    @JoinColumn(name = "acteur_id")
    private Utilisateur acteur;

    @ManyToOne
    @JoinColumn(name = "stagiaire_id")
    private Stagiaire stagiaire;

    @Column(nullable = false)
    private String description;
}
