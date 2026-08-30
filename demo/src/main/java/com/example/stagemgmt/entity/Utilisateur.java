package com.example.stagemgmt.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt-encoded

    @Column(nullable = false)
    private String nomComplet;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** École / université du stagiaire (non applicable pour un responsable). */
    private String ecole;

    @Builder.Default
    private boolean actif = true;

    /** Photo de profil sous forme de "data:image/...;base64,..." - gardée hors des
     *  API/listes en masse (pas de @JsonIgnore nécessaire ici puisque cette appli
     *  ne sert pas de JSON, tout est rendu côté serveur). */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photoProfil;

    @Builder.Default
    private boolean twoFactorEnabled = false;

    /** Secret TOTP en base32. Null tant que la 2FA n'est pas activée. */
    private String twoFactorSecret;

    /** Identifiants stables des notifications que l'utilisateur a cliquées/masquées.
     *  Table séparée (élément collection JPA) plutôt qu'un blob CSV - reste propre
     *  même si la liste grossit. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notifications_masquees", joinColumns = @JoinColumn(name = "utilisateur_id"))
    @Column(name = "notification_id")
    @Builder.Default
    private java.util.Set<String> notificationsMasquees = new java.util.HashSet<>();
}
