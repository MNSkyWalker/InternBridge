package com.example.stagemgmt.config;

import com.example.stagemgmt.entity.*;
import com.example.stagemgmt.repository.EvaluationRepository;
import com.example.stagemgmt.repository.LivrableRepository;
import com.example.stagemgmt.repository.StagiaireRepository;
import com.example.stagemgmt.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Jeu de données de démo pour pouvoir tester l'appli dès le premier lancement.
 *  Désactiver avec app.seed-demo-data=false une fois que vous avez vos propres données.
 *
 *  Deux rôles se connectent : responsable et encadreur. Les stagiaires ne sont que
 *  des fiches de données (pas de compte, pas de mot de passe), créées et gérées
 *  par le responsable (et éditées par l'encadreur qui leur est assigné). */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UtilisateurRepository utilisateurRepository;
    private final StagiaireRepository stagiaireRepository;
    private final LivrableRepository livrableRepository;
    private final EvaluationRepository evaluationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-data:false}")
    private boolean seedEnabled;

    public DataSeeder(UtilisateurRepository utilisateurRepository, StagiaireRepository stagiaireRepository,
                       LivrableRepository livrableRepository, EvaluationRepository evaluationRepository,
                       PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.stagiaireRepository = stagiaireRepository;
        this.livrableRepository = livrableRepository;
        this.evaluationRepository = evaluationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled || utilisateurRepository.count() > 0) return;

        Utilisateur responsable1 = save("responsable1", "Nadia Trabelsi", "nadia.trabelsi@banque.tn", "Responsable#123", Role.RESPONSABLE);
        save("responsable2", "Ahmed Ben Salah", "ahmed.bensalah@banque.tn", "Responsable#123", Role.RESPONSABLE);
        Utilisateur encadreur1 = save("encadreur1", "Sami Jendoubi", "sami.jendoubi@banque.tn", "Encadreur#123", Role.ENCADREUR);

        Stagiaire yassine = stagiaireRepository.save(Stagiaire.builder()
                .nomComplet("Yassine Gharbi")
                .email("yassine.gharbi@etu.tn")
                .ecole("ESPRIT")
                .anneeInscription(2026)
                .filiere("Génie Logiciel")
                .cycle(CycleEtudes.INGENIEUR)
                .titreSujet("Application de gestion des stagiaires")
                .descriptionSujet("Développement d'une application Spring Boot pour le suivi des stages, avec pipeline CI/CD Jenkins.")
                .dateDebut(LocalDate.now().minusWeeks(1))
                .dateFin(LocalDate.now().plusWeeks(4))
                .responsable(responsable1)
                .encadreur(encadreur1)
                .build());

        stagiaireRepository.save(Stagiaire.builder()
                .nomComplet("Mariem Khiari")
                .email("mariem.khiari@etu.tn")
                .ecole("INSAT")
                .anneeInscription(2026)
                .filiere("Réseaux et Télécommunications")
                .cycle(CycleEtudes.LICENCE)
                .titreSujet("Supervision réseau interne")
                .descriptionSujet("Mise en place d'outils de supervision pour l'infrastructure réseau de l'agence.")
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusMonths(2))
                .responsable(responsable1)
                .build());

        livrableRepository.save(Livrable.builder().titre("Cahier des charges").statut(StatutLivrable.TERMINE).avancement(100)
                .dateEcheance(LocalDate.now().minusDays(3)).stagiaire(yassine).build());
        livrableRepository.save(Livrable.builder().titre("Modèle de données + entités JPA").statut(StatutLivrable.DEVELOPPEMENT).avancement(60)
                .dateEcheance(LocalDate.now().plusDays(4)).stagiaire(yassine).build());
        livrableRepository.save(Livrable.builder().titre("Pipeline Jenkins (build + tests)").statut(StatutLivrable.CADRAGE).avancement(0)
                .dateEcheance(LocalDate.now().plusWeeks(3)).stagiaire(yassine).build());

        evaluationRepository.save(Evaluation.builder().stagiaire(yassine).dateEvaluation(LocalDate.now().minusDays(2))
                .note(15).commentaire("Bonne compréhension du besoin, cahier des charges clair.").finale(false).build());

        log.warn("=============================================================");
        log.warn(" Données de démo créées. Comptes de test :");
        log.warn("   responsable1 / Responsable#123  (gère Yassine et Mariem)");
        log.warn("   responsable2 / Responsable#123  (aucun stagiaire pour l'instant)");
        log.warn("   encadreur1 / Encadreur#123  (encadre Yassine)");
        log.warn(" Désactivez ce seed avec app.seed-demo-data=false");
        log.warn("=============================================================");
    }

    private Utilisateur save(String username, String nomComplet, String email, String rawPassword, Role role) {
        return utilisateurRepository.save(Utilisateur.builder()
                .username(username)
                .nomComplet(nomComplet)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .actif(true)
                .build());
    }
}
