package com.example.stagemgmt.controller;

import com.example.stagemgmt.service.ChatbotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Accessible aux deux rôles (stagiaire et responsable) - pas de données personnelles
 *  impliquées, juste un assistant d'aide à l'utilisation de l'application. */
@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public Map<String, String> envoyer(@RequestParam String message, HttpSession session) {
        return Map.of("reponse", chatbotService.repondre(session, message));
    }

    @PostMapping("/reinitialiser")
    public void reinitialiser(HttpSession session) {
        chatbotService.reinitialiser(session);
    }
}
