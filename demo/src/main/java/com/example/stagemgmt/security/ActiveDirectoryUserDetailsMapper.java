package com.example.stagemgmt.security;

import com.example.stagemgmt.entity.Role;
import com.example.stagemgmt.entity.Utilisateur;
import com.example.stagemgmt.repository.UtilisateurRepository;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

/**
 * Fait le pont entre l'Active Directory et le modèle de données de l'appli : une fois
 * qu'un responsable s'est authentifié auprès de l'AD, on cherche (ou on crée au premier
 * login) sa fiche Utilisateur locale, pour que tout le reste (2FA, photo de profil,
 * propriété des fiches stagiaires, notifications) continue de fonctionner exactement
 * comme avec un compte local - en renvoyant le même type UtilisateurPrincipal des
 * deux côtés.
 *
 * Le mot de passe stocké localement pour un compte provisionné depuis l'AD est un
 * secret aléatoire inutilisable : l'AD reste la seule source de vérité pour
 * l'authentification, cette appli ne le compare jamais.
 */
public class ActiveDirectoryUserDetailsMapper implements UserDetailsContextMapper {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public ActiveDirectoryUserDetailsMapper(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                           Collection<? extends GrantedAuthority> authorities) {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseGet(() -> provisionner(ctx, username));
        return new UtilisateurPrincipal(utilisateur);
    }

    private Utilisateur provisionner(DirContextOperations ctx, String username) {
        String nomComplet = premierNonVide(ctx.getStringAttribute("displayName"), ctx.getStringAttribute("cn"), username);
        String email = ctx.getStringAttribute("mail");

        Utilisateur nouveau = Utilisateur.builder()
                .username(username)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .nomComplet(nomComplet)
                .email(email != null ? email : username + "@banque.local")
                .role(Role.RESPONSABLE)
                .actif(true)
                .build();
        return utilisateurRepository.save(nouveau);
    }

    private String premierNonVide(String... valeurs) {
        for (String v : valeurs) {
            if (v != null && !v.isBlank()) return v;
        }
        return "Responsable";
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("Lecture seule : cette appli ne modifie jamais l'annuaire AD");
    }
}
