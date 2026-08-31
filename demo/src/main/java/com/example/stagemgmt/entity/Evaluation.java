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

    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;

    /** Note sur 20, comme c'est l'usage académique/RH courant en France/Tunisie. */
    private Integer note;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String commentaire;

    @Column(columnDefinition = "NUMBER(1,0)")
    private boolean finale;
}
