package com.example.stagemgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Évaluation (intermédiaire ou finale) rédigée par le responsable pour un stagiaire. */
@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stagiaire_id")
    private Stagiaire stagiaire;

    @Column(nullable = false)
    private LocalDate date;

    /** Note sur 20, comme c'est l'usage académique/RH courant en France/Tunisie. */
    private Integer note;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    private boolean finale;
}
