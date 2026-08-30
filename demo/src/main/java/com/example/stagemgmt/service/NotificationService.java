package com.example.stagemgmt.service;

import com.example.stagemgmt.dto.Notification;
import com.example.stagemgmt.entity.*;
import com.example.stagemgmt.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Calcule les notifications à la volée (l'aspect "alarme" demandé) : échéances de
 * livrables et réunions à venir, pour tous les stagiaires gérés par le responsable
 * connecté. Comme le responsable saisit lui-même tout l'avancement, il n'y a plus
 * besoin de notifier "un stagiaire a mis à jour son avancement" - il le sait déjà,
 * puisque c'est lui qui vient de le faire.
 */
@Service
public class NotificationService {

    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("dd/MM à HH:mm");

    private final StagiaireService stagiaireService;
    private final LivrableService livrableService;
    private final ReunionService reunionService;
    private final UtilisateurRepository utilisateurRepository;

    public NotificationService(StagiaireService stagiaireService, LivrableService livrableService,
                                ReunionService reunionService, UtilisateurRepository utilisateurRepository) {
        this.stagiaireService = stagiaireService;
        this.livrableService = livrableService;
        this.reunionService = reunionService;
        this.utilisateurRepository = utilisateurRepository;
    }

    public List<Notification> pourResponsable(String username) {
        List<Notification> notifications = new ArrayList<>();
        for (Stagiaire stagiaire : stagiaireService.findByResponsable(username)) {
            String lien = "/responsable/stagiaires/" + stagiaire.getId();
            for (Livrable l : livrableService.findByStagiaire(stagiaire.getId())) {
                ajouterAlerteEcheance(notifications, l, stagiaire.getNomComplet() + " — " + l.getTitre(), lien);
            }
        }
        for (Reunion r : reunionService.aVenirPourResponsable(username)) {
            ajouterAlerteReunion(notifications, r);
        }
        return filtrerEtTrier(notifications, username);
    }

    /** Masque une notification pour un utilisateur donné - persisté, donc ça reste
     *  masqué à la prochaine connexion aussi. */
    @Transactional
    public void masquer(String username, String notificationId) {
        Utilisateur u = utilisateurRepository.findByUsername(username).orElseThrow();
        u.getNotificationsMasquees().add(notificationId);
        utilisateurRepository.save(u);
    }

    private void ajouterAlerteEcheance(List<Notification> notifications, Livrable l, String label, String lien) {
        if (l.getStatut() == StatutLivrable.TERMINE || l.getDateEcheance() == null) return;

        long jours = ChronoUnit.DAYS.between(LocalDate.now(), l.getDateEcheance());
        LocalDateTime quand = l.getDateEcheance().atStartOfDay();
        String base = "livrable-" + l.getId();

        if (jours < 0) {
            notifications.add(new Notification(base + "-en-retard", Notification.Type.ALARME,
                    "En retard : " + label + " (échéance dépassée de " + (-jours) + " j)", lien, quand));
        } else if (jours == 0) {
            notifications.add(new Notification(base + "-aujourdhui", Notification.Type.ALARME,
                    "Échéance aujourd'hui : " + label, lien, quand));
        } else if (jours <= 2) {
            notifications.add(new Notification(base + "-proche", Notification.Type.ALARME,
                    "Échéance proche : " + label + " (dans " + jours + " j)", lien, quand));
        } else if (jours <= 5) {
            notifications.add(new Notification(base + "-a-venir", Notification.Type.ATTENTION,
                    "À venir : " + label + " (dans " + jours + " j)", lien, quand));
        }
    }

    private void ajouterAlerteReunion(List<Notification> notifications, Reunion r) {
        long heures = Duration.between(LocalDateTime.now(), r.getDateHeure()).toHours();
        String label = r.getStagiaire().getNomComplet() + " — " + r.getDateHeure().format(FORMAT_HEURE);
        String lien = "/responsable/reunions";
        String base = "reunion-" + r.getId();

        if (heures <= 24) {
            notifications.add(new Notification(base + "-bientot", Notification.Type.ALARME,
                    "Réunion bientôt : " + label, lien, r.getDateHeure()));
        } else if (heures <= 72) {
            notifications.add(new Notification(base + "-a-venir", Notification.Type.ATTENTION,
                    "Réunion à venir : " + label, lien, r.getDateHeure()));
        }
    }

    private List<Notification> filtrerEtTrier(List<Notification> notifications, String username) {
        Set<String> masquees = utilisateurRepository.findByUsername(username)
                .map(Utilisateur::getNotificationsMasquees)
                .orElse(Set.of());

        Comparator<Notification> parType = Comparator.comparingInt(n -> switch (n.getType()) {
            case ALARME -> 0;
            case ATTENTION -> 1;
            case INFO -> 2;
        });

        return notifications.stream()
                .filter(n -> !masquees.contains(n.getId()))
                .sorted(parType.thenComparing(Comparator.comparing(Notification::getDate).reversed()))
                .toList();
    }
}
