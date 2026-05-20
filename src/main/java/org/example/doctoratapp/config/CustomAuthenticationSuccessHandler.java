package org.example.doctoratapp.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String redirectUrl = "/dashboard";

        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_CANDIDAT"))) {
            redirectUrl = "/dashboard/candidat";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTEUR"))) {
            redirectUrl = "/dashboard/directeur";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            redirectUrl = "/dashboard/admin";
        }

        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
