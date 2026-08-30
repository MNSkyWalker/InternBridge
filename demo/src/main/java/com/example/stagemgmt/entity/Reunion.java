package com.example.stagemgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Un point de suivi planifié avec un stagiaire (entretien, soutenance intermédiaire...). */
@Entity
@Table(name = "reunions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reunion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    private String lieu;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutReunion statut = StatutReunion.PLANIFIEE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stagiaire_id")
    private Stagiaire stagiaire;
}
