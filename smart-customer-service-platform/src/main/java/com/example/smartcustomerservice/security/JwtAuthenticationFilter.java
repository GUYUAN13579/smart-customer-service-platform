package com.example.smartcustomerservice.security;

import com.example.smartcustomerservice.common.constants.RedisKeyConstants;
import com.example.smartcustomerservice.common.constants.SecurityConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   StringRedisTemplate stringRedisTemplate,
                                   ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtTokenProvider.isAccessToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Long userId = jwtTokenProvider.getUserId(token);
            String username = jwtTokenProvider.getUsername(token);
            List<String> roleCodes = jwtTokenProvider.getRoleCodes(token);
            List<String> permissionCodes = getPermissionCodes(userId);
            List<SimpleGrantedAuthority> authorities = permissionCodes.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            LoginUserContext principal = new LoginUserContext(userId, username, roleCodes, permissionCodes);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(SecurityConstants.BEARER_PREFIX.length());
    }

    private List<String> getPermissionCodes(Long userId) throws IOException {
        String permissionKey = RedisKeyConstants.format(RedisKeyConstants.USER_PERMISSION, userId);
        String permissionJson = stringRedisTemplate.opsForValue().get(permissionKey);
        if (!StringUtils.hasText(permissionJson)) {
            return List.of();
        }
        return objectMapper.readValue(permissionJson, new TypeReference<List<String>>() {
        });
    }
}
