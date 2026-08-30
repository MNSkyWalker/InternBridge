package com.example.stagemgmt.service;

import com.example.stagemgmt.entity.CategorieDocument;
import com.example.stagemgmt.entity.DocumentStagiaire;
import com.example.stagemgmt.entity.Stagiaire;
import com.example.stagemgmt.repository.DocumentStagiaireRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class DocumentService {

    private static final long TAILLE_MAX = 10L * 1024 * 1024; // 10 Mo
    private static final Set<String> TYPES_AUTORISES = Set.of(
            "application/pdf", "image/jpeg", "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final DocumentStagiaireRepository documentRepository;

    public DocumentService(DocumentStagiaireRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public List<DocumentStagiaire> findByStagiaire(Long stagiaireId) {
        return documentRepository.findByStagiaireIdOrderByDateAjoutDesc(stagiaireId);
    }

    public DocumentStagiaire findById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable"));
    }

    @Transactional
    public DocumentStagiaire ajouter(Stagiaire stagiaire, MultipartFile fichier, CategorieDocument categorie) {
        if (fichier.isEmpty()) {
            throw new IllegalArgumentException("Veuillez choisir un fichier");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new IllegalArgumentException("Le fichier doit faire 10 Mo maximum");
        }
        String type = fichier.getContentType();
        if (type == null || !TYPES_AUTORISES.contains(type)) {
            throw new IllegalArgumentException("Formats acceptés : PDF, Word, JPEG, PNG");
        }

        try {
            DocumentStagiaire document = DocumentStagiaire.builder()
                    .nomFichier(fichier.getOriginalFilename())
                    .typeMime(type)
                    .tailleOctets(fichier.getSize())
                    .categorie(categorie == null ? CategorieDocument.AUTRE : categorie)
                    .dateAjout(LocalDateTime.now())
                    .contenu(fichier.getBytes())
                    .stagiaire(stagiaire)
                    .build();
            return documentRepository.save(document);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier envoyé");
        }
    }

    @Transactional
    public void supprimer(Long id) {
        documentRepository.deleteById(id);
    }
}
