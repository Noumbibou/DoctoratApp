package org.example.doctoratapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ConfigSecurity {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomAuthenticationSuccessHandler successHandler) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers((RequestMatcher) req -> {
                    String uri = req.getRequestURI();
                    return uri.startsWith("/api/") || uri.startsWith("/dev/");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/campagnes/**", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/dev/**").permitAll()
                .requestMatchers("/dossiers/validation/**").hasRole("DIRECTEUR")
                .requestMatchers("/dashboard/admin").hasRole("ADMIN")
                .requestMatchers("/dashboard/directeur").hasRole("DIRECTEUR")
                .requestMatchers("/dashboard/candidat").hasRole("CANDIDAT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email") // Important car ton formulaire utilise 'email'
                .passwordParameter("motDePasse") // Important car ton formulaire utilise 'motDePasse'
                .successHandler(successHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
