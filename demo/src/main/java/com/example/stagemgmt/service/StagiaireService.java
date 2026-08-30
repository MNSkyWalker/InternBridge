package com.example.stagemgmt.service;

import com.example.stagemgmt.entity.Role;
import com.example.stagemgmt.entity.Stagiaire;
import com.example.stagemgmt.repository.StagiaireRepository;
import com.example.stagemgmt.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StagiaireService {

    private final StagiaireRepository stagiaireRepository;
    private final UtilisateurRepository utilisateurRepository;

    public StagiaireService(StagiaireRepository stagiaireRepository, UtilisateurRepository utilisateurRepository) {
        this.stagiaireRepository = stagiaireRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /** Malgré le nom, couvre aussi les encadreurs : renvoie les stagiaires que cet
     *  utilisateur gère, qu'il soit responsable (fiches qu'il a créées) ou
     *  encadreur (fiches où il est assigné comme superviseur). */
    public List<Stagiaire> findByResponsable(String username) {
        Role role = utilisateurRepository.findByUsername(username).map(u -> u.getRole()).orElse(Role.RESPONSABLE);
        if (role == Role.ENCADREUR) {
            return stagiaireRepository.findByEncadreurUsernameOrderByNomCompletAsc(username);
        }
        return stagiaireRepository.findByResponsableUsernameOrderByNomCompletAsc(username);
    }

    public Stagiaire findById(Long id) {
        return stagiaireRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stagiaire introuvable"));
    }

    @Transactional
    public Stagiaire creer(Stagiaire stagiaire) {
        return stagiaireRepository.save(stagiaire);
    }

    /** Vérifie que cet utilisateur (responsable ou encadreur assigné) gère bien ce
     *  stagiaire, sinon lève une exception. */
    public void verifierProprietaire(Stagiaire stagiaire, String username) {
        if (!stagiaire.estGerePar(username)) {
            throw new SecurityException("Vous ne gérez pas ce stagiaire");
        }
    }

    @Transactional
    public Stagiaire enregistrer(Stagiaire stagiaire) {
        return stagiaireRepository.save(stagiaire);
    }
}
