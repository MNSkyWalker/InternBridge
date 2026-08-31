package com.example.stagemgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un jalon/livrable du stage (ex: "Cahier des charges", "Prototype v1").
 *  Le responsable saisit et met à jour l'avancement lui-même. */
@Entity
@Table(name = "livrables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String description;

    private LocalDate dateEcheance;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutLivrable statut = StatutLivrable.CADRAGE;

    /** Pourcentage 0-100, saisi par le responsable. */
    @Builder.Default
    private int avancement = 0;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stagiaire_id")
    private Stagiaire stagiaire;

    /** Utilisées pour générer les notifications (nouveau livrable / mise à jour récente). */
    private LocalDateTime dateCreation;
    private LocalDateTime derniereMiseAJour;
}
