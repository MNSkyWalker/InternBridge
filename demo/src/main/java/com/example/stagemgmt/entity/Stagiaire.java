package com.example.stagemgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Fiche stagiaire : un enregistrement de données géré par le responsable et/ou
 * l'encadreur qui lui est assigné (le stagiaire n'a pas de compte, ne se connecte
 * pas). Regroupe l'identité de l'étudiant ET le sujet de stage (fusionnés en une
 * seule entité depuis que le suivi est fait manuellement plutôt que par le
 * stagiaire lui-même).
 */
@Entity
@Table(name = "stagiaires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stagiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- Identité de l'étudiant ----
    @Column(nullable = false)
    private String nomComplet;

    private String email;

    private String ecole;

    /** Année d'inscription / promotion, ex: 2026. */
    private Integer anneeInscription;

    /** Filière / profession visée, ex: "Génie Logiciel". */
    private String filiere;

    @Enumerated(EnumType.STRING)
    private CycleEtudes cycle;

    // ---- Sujet de stage ----
    @Column(nullable = false)
    private String titreSujet;

    @Column(columnDefinition = "TEXT")
    private String descriptionSujet;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    @ManyToOne(optional = false)
    @JoinColumn(name = "responsable_id")
    private Utilisateur responsable;

    /** Encadreur (superviseur bancaire, compte avec le rôle ENCADREUR) assigné par
     *  le responsable, optionnel. */
    @ManyToOne
    @JoinColumn(name = "encadreur_id")
    private Utilisateur encadreur;

    /** Vrai si cet utilisateur (responsable OU encadreur assigné) a le droit de
     *  gérer ce stagiaire. */
    public boolean estGerePar(String username) {
        boolean estResponsable = responsable != null && responsable.getUsername().equals(username);
        boolean estEncadreur = encadreur != null && encadreur.getUsername().equals(username);
        return estResponsable || estEncadreur;
    }
}
