package com.example.stagemgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Un fichier attaché à la fiche d'un stagiaire (CV, convention de stage, rapport...).
 *  Nommé DocumentStagiaire plutôt que Document pour éviter toute confusion avec
 *  d'autres classes "Document" du JDK/des libs. */
@Entity
@Table(name = "documents_stagiaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentStagiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomFichier;

    @Column(nullable = false)
    private String typeMime;

    private long tailleOctets;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CategorieDocument categorie = CategorieDocument.AUTRE;

    @Column(nullable = false)
    private LocalDateTime dateAjout;

    /** columnDefinition forcé en LONGBLOB : sans ça, Hibernate + MySQL créent parfois
     *  une colonne BLOB standard limitée à 64 Ko, ce qui fait échouer l'enregistrement
     *  de tout fichier réel (PDF, Word...) dès qu'il dépasse cette taille. */
    @Lob
    @Column(nullable = false, columnDefinition = "BLOB")
    private byte[] contenu;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stagiaire_id")
    private Stagiaire stagiaire;
}
