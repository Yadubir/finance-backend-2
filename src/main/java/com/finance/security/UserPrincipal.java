package com.finance.security;

import com.finance.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts our User entity to Spring Security's UserDetails contract.
 *
 * Spring Security uses UserDetails internally for authentication and
 * authorization decisions. This adapter avoids coupling our User entity
 * directly to Spring Security interfaces.
 *
 * The role is prefixed with "ROLE_" per Spring Security convention,
 * which is what @PreAuthorize("hasRole('ADMIN')") checks against.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public Long getId()    { return user.getId(); }
    public String getRole() { return user.getRole().name(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Single role per user. Returns "ROLE_ADMIN", "ROLE_ANALYST", or "ROLE_VIEWER"
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() { return user.getPasswordHash(); }

    @Override
    public String getUsername() { return user.getEmail(); }

    // Account status checks — tied to our UserStatus and soft-delete fields
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return user.isActive(); }
}
