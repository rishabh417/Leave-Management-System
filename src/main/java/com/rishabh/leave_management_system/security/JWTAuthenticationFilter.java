package com.rishabh.leave_management_system.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    public JWTService jwtService;
    @Autowired
    public UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        try {

        String jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if(jwtService.isTokenValid(jwt,userDetails)){
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authenticationToken);
            }
            else{
                response.setStatus(401);
                response.getWriter().println("Invalid JWT token");
                return;
            }
        } catch (JwtException e) {
            response.setStatus(401);
            response.getWriter().println("Invalid JWT token");
            return;
        }

        filterChain.doFilter(request,response);

    }


}
