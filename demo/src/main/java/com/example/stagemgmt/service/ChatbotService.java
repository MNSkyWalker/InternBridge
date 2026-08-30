package com.example.stagemgmt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Assistant conversationnel via l'API Groq (https://console.groq.com), qui a un vrai
 * palier gratuit permanent (pas juste un essai limité dans le temps) - aucune carte
 * bancaire requise. L'API est compatible avec le format OpenAI (POST /chat/completions),
 * ce qui rend l'appel HTTP simple.
 *
 * Volontairement, l'assistant n'a PAS accès aux données personnelles de l'utilisateur
 * (avancement, notes, etc.) - c'est un choix délibéré pour ne rien envoyer de sensible
 * à un service tiers. Il aide à utiliser l'application et répond à des questions
 * générales, rien de plus.
 */
@Service
public class ChatbotService {

    private static final int MAX_HISTORIQUE = 10; // messages gardés en session, pour limiter les tokens envoyés
    private static final String SESSION_KEY = "chatHistorique";

    private static final String SYSTEM_PROMPT = """
            Tu es l'assistant virtuel d'InternBridge, une application de suivi de stages pour une banque.
            Un seul type d'utilisateur s'y connecte : le responsable / maître de stage. Il y gère ses
            stagiaires (fiche : nom, école, année d'inscription, filière, cycle Licence/Ingénieur, sujet
            et période de stage), suit l'avancement des livrables par étapes Cadrage / Développement /
            Tests / Terminé, et rédige des évaluations notées sur 20. Les stagiaires eux-mêmes n'ont pas
            de compte - tout est saisi par le responsable.
            Réponds toujours en français, de façon brève, claire et concrète (quelques phrases maximum).
            Tu peux expliquer comment utiliser l'application ou donner des conseils généraux sur le
            déroulement et l'encadrement d'un stage.
            Tu n'as PAS accès aux données personnelles stockées dans l'application (fiches stagiaires,
            avancement, notes) - si on te demande une information spécifique, réponds que tu n'y as pas
            accès et invite à consulter le tableau de bord.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public ChatbotService(@Value("${app.chatbot.api-key:}") String apiKey,
                           @Value("${app.chatbot.model:llama-3.3-70b-versatile}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .build();
    }

    public boolean estConfigure() {
        return apiKey != null && !apiKey.isBlank();
    }

    @SuppressWarnings("unchecked")
    public String repondre(HttpSession session, String messageUtilisateur) {
        if (!estConfigure()) {
            return "L'assistant n'est pas encore configuré. Un administrateur doit ajouter une clé API "
                    + "Groq gratuite (console.groq.com) dans application.properties.";
        }
        if (messageUtilisateur == null || messageUtilisateur.isBlank()) {
            return "Posez-moi une question !";
        }

        List<Map<String, String>> historique = (List<Map<String, String>>) session.getAttribute(SESSION_KEY);
        if (historique == null) {
            historique = new ArrayList<>();
        }
        historique.add(Map.of("role", "user", "content", messageUtilisateur));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.addAll(historique);

        Map<String, Object> corps = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.4,
                "max_tokens", 400
        );

        try {
            String reponseBrute = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corps)
                    .retrieve()
                    .body(String.class);

            JsonNode racine = objectMapper.readTree(reponseBrute);
            String reponseTexte = racine.path("choices").get(0).path("message").path("content").asText();

            historique.add(Map.of("role", "assistant", "content", reponseTexte));
            while (historique.size() > MAX_HISTORIQUE) {
                historique.remove(0);
            }
            session.setAttribute(SESSION_KEY, historique);

            return reponseTexte;
        } catch (Exception e) {
            return "Désolé, je n'ai pas pu répondre pour le moment. Réessayez dans un instant.";
        }
    }

    public void reinitialiser(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }
}
