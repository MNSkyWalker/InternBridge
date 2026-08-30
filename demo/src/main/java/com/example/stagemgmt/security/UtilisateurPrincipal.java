package com.example.stagemgmt.security;

import com.example.stagemgmt.entity.Utilisateur;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Adapte notre entité Utilisateur au contrat UserDetails attendu par Spring Security. */
@Getter
public class UtilisateurPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String nomComplet;
    private final String password;
    private final String role;
    private final boolean actif;
    private final boolean twoFactorEnabled;

    public UtilisateurPrincipal(Utilisateur u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.nomComplet = u.getNomComplet();
        this.password = u.getPassword();
        this.role = u.getRole().name();
        this.actif = u.isActif();
        this.twoFactorEnabled = u.isTwoFactorEnabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return actif; }
}
