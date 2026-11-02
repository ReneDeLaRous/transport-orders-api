package com.transportes.api.security;

import com.transportes.api.user.AppUser;
import com.transportes.api.user.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public AppUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AppUser> found = userRepository.findByUsername(username);
        if (found.isEmpty()) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }

        AppUser appUser = found.get();
        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .roles("USER")
                .build();
    }
}
