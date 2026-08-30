package com.example.stagemgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** Une notification calculée à la volée (pas stockée en base) - seul l'identifiant
 *  "masqué" (voir Utilisateur.notificationsMasquees) est persisté, pas la notification
 *  elle-même. `id` est stable dans le temps pour un même événement (ex: même livrable +
 *  même palier d'échéance), pour qu'un clic la masque durablement - mais une
 *  escalade (ex: passe de "à venir" à "en retard") génère un nouvel id, donc réapparaît. */
@Getter
@AllArgsConstructor
public class Notification {

    public enum Type {
        ALARME,   // échéance dépassée ou très proche - rouge
        ATTENTION, // échéance proche - orange
        INFO      // nouveauté / mise à jour normale - bleu
    }

    private final String id;
    private final Type type;
    private final String message;
    private final String lien;
    private final LocalDateTime date;
}
