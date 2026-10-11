package org.adso.minimarket.config;

import org.adso.minimarket.models.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public Long getId() {
        return user != null ? user.getId() : null;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "user=" + "{" +
                "id: " + user.getId() +
                "email: " + user.getEmail() +
                "role: " + user.getRole() +
                '}' +
                '}';
    }
}