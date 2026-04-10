package com.tns.appraisal.config;

import com.tns.appraisal.user.User;
import com.tns.appraisal.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that reads the authenticated user ID from the HTTP session and
 * populates the Spring Security SecurityContext so that @PreAuthorize works.
 * Caches granted authorities in the session to avoid a DB hit on every request.
 */
@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    private static final String SESSION_USER_KEY = "AUTHENTICATED_USER_ID";
    private static final String SESSION_AUTHORITIES_KEY = "AUTHENTICATED_USER_AUTHORITIES";

    private final UserRepository userRepository;

    public SessionAuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Long userId = (Long) session.getAttribute(SESSION_USER_KEY);

            if (userId != null) {
                // Use cached authorities from session to avoid DB hit on every request
                @SuppressWarnings("unchecked")
                List<SimpleGrantedAuthority> cachedAuthorities =
                        (List<SimpleGrantedAuthority>) session.getAttribute(SESSION_AUTHORITIES_KEY);

                if (cachedAuthorities != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, cachedAuthorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // First request after login — load from DB and cache
                    userRepository.findById(userId).ifPresent(user -> {
                        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                                .collect(Collectors.toList());

                        session.setAttribute(SESSION_AUTHORITIES_KEY, authorities);

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
