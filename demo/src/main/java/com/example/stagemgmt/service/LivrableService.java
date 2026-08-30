package com.example.stagemgmt.service;

import com.example.stagemgmt.entity.Livrable;
import com.example.stagemgmt.entity.StatutLivrable;
import com.example.stagemgmt.repository.LivrableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LivrableService {

    private final LivrableRepository livrableRepository;

    public LivrableService(LivrableRepository livrableRepository) {
        this.livrableRepository = livrableRepository;
    }

    public List<Livrable> findByStagiaire(Long stagiaireId) {
        return livrableRepository.findByStagiaireIdOrderByDateEcheanceAsc(stagiaireId);
    }

    public Livrable findById(Long id) {
        return livrableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Livrable introuvable"));
    }

    @Transactional
    public Livrable ajouter(Livrable livrable) {
        LocalDateTime now = LocalDateTime.now();
        livrable.setDateCreation(now);
        livrable.setDerniereMiseAJour(now);
        return livrableRepository.save(livrable);
    }

    /** Le responsable (ou l'encadreur assigné) met à jour l'avancement lui-même
     *  (plus d'auto-déclaration par le stagiaire, qui n'a plus de compte). Vérifie
     *  que ce livrable appartient bien à un stagiaire géré par cet utilisateur. */
    @Transactional
    public Livrable mettreAJourAvancement(Long id, int avancement, StatutLivrable statut, String username) {
        Livrable livrable = findById(id);
        if (!livrable.getStagiaire().estGerePar(username)) {
            throw new SecurityException("Vous ne gérez pas ce stagiaire");
        }
        livrable.setAvancement(Math.max(0, Math.min(100, avancement)));
        livrable.setStatut(statut);
        livrable.setDerniereMiseAJour(LocalDateTime.now());
        return livrableRepository.save(livrable);
    }

    /** Moyenne d'avancement de tous les livrables d'un stagiaire (0 si aucun livrable). */
    public int avancementMoyen(Long stagiaireId) {
        List<Livrable> livrables = findByStagiaire(stagiaireId);
        if (livrables.isEmpty()) return 0;
        return (int) Math.round(livrables.stream().mapToInt(Livrable::getAvancement).average().orElse(0));
    }

    @Transactional
    public void supprimer(Long id) {
        livrableRepository.deleteById(id);
    }

    /** L'étape la plus avancée parmi les livrables du stagiaire (CADRAGE par défaut si
     *  aucun livrable) - utilisé pour le filtre par statut dans "Mes stagiaires". */
    public StatutLivrable statutGlobal(Long stagiaireId) {
        return findByStagiaire(stagiaireId).stream()
                .map(Livrable::getStatut)
                .max(java.util.Comparator.comparingInt(Enum::ordinal))
                .orElse(StatutLivrable.CADRAGE);
    }
}
