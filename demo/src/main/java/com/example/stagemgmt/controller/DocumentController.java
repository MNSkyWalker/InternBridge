package com.example.stagemgmt.controller;

import com.example.stagemgmt.entity.CategorieDocument;
import com.example.stagemgmt.entity.DocumentStagiaire;
import com.example.stagemgmt.entity.Stagiaire;
import com.example.stagemgmt.service.DocumentService;
import com.example.stagemgmt.service.JournalActiviteService;
import com.example.stagemgmt.service.StagiaireService;
import com.example.stagemgmt.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pièces jointes d'une fiche stagiaire (CV, convention de stage, rapport...).
 *  Toujours vérifié : le document appartient bien à un stagiaire géré par le
 *  responsable connecté, avant lecture ou suppression. */
@Controller
@RequestMapping("/responsable")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final StagiaireService stagiaireService;
    private final UtilisateurRepository utilisateurRepository;
    private final JournalActiviteService journalActiviteService;

    @PostMapping("/stagiaires/{id}/documents")
    public String televerser(@PathVariable Long id,
                              @RequestParam("fichier") MultipartFile fichier,
                              @RequestParam(required = false) CategorieDocument categorie,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        Stagiaire stagiaire = stagiaireService.findById(id);
        stagiaireService.verifierProprietaire(stagiaire, authentication.getName());

        try {
            documentService.ajouter(stagiaire, fichier, categorie);
            utilisateurRepository.findByUsername(authentication.getName()).ifPresent(u ->
                    journalActiviteService.enregistrer(u, stagiaire, "A ajouté le document \"" + fichier.getOriginalFilename() + "\""));
            redirectAttributes.addFlashAttribute("success", "Document ajouté avec succès !");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            // Filet de sécurité : évite la page d'erreur générique blanche pour toute
            // panne imprévue (ex. base de données), et redirige avec un message clair.
            redirectAttributes.addFlashAttribute("error", "Le document n'a pas pu être enregistré. Réessayez.");
        }
        return "redirect:/responsable/stagiaires/" + id;
    }

    @GetMapping("/documents/{documentId}/telecharger")
    @ResponseBody
    public ResponseEntity<byte[]> telecharger(@PathVariable Long documentId, Authentication authentication) {
        DocumentStagiaire document = documentService.findById(documentId);
        stagiaireService.verifierProprietaire(document.getStagiaire(), authentication.getName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getNomFichier()).build().toString())
                .body(document.getContenu());
    }

    @PostMapping("/documents/{documentId}/supprimer")
    public String supprimer(@PathVariable Long documentId, Authentication authentication, RedirectAttributes redirectAttributes) {
        DocumentStagiaire document = documentService.findById(documentId);
        stagiaireService.verifierProprietaire(document.getStagiaire(), authentication.getName());
        Long stagiaireId = document.getStagiaire().getId();

        documentService.supprimer(documentId);
        redirectAttributes.addFlashAttribute("success", "Document supprimé.");
        return "redirect:/responsable/stagiaires/" + stagiaireId;
    }
}
