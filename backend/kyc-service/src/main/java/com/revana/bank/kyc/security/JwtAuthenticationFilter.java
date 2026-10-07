package com.revana.bank.kyc.security;


import com.revana.bank.kyc.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }


        String token =
                authHeader.substring(7);
        System.out.println("Token Received = " + token);


        if (jwtService.isTokenValid(token)) {

            String username =
                    jwtService.extractUsername(token);

            String role =
                    jwtService.extractRole(token);
            System.out.println("Username = " + username);
            System.out.println("Role = " + role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            java.util.List.of(
                                    new org.springframework.security.core.authority
                                            .SimpleGrantedAuthority(
                                            "ROLE_" + role)));

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request));

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            System.out.println(
                    "Authorities = "
                            + authentication.getAuthorities());

            System.out.println(
                    "Authentication = "
                            + SecurityContextHolder
                            .getContext()
                            .getAuthentication());
        }



        filterChain.doFilter(request, response);
    }
}
