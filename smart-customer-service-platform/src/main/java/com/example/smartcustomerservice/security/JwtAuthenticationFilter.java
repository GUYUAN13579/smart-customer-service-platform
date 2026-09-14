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
// JwtAuthenticationFilter 属于智能客服平台基础代码。
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
            // refreshToken 只用于换取新的 accessToken，不能被当作业务接口凭证使用。
            // 在过滤器层直接拒绝可以避免 refreshToken 被误放入 SecurityContext。
            if (!jwtTokenProvider.isAccessToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Long userId = jwtTokenProvider.getUserId(token);
            String username = jwtTokenProvider.getUsername(token);
            List<String> roleCodes = jwtTokenProvider.getRoleCodes(token);
            // 认证阶段只解析轻量身份信息；权限从 Redis 读取，避免每个请求重复查询
            // user-role-permission 三张关联表。缓存缺失时返回空权限，受保护接口仍会被拒绝。
            List<String> permissionCodes = getPermissionCodes(userId);
            List<SimpleGrantedAuthority> authorities = permissionCodes.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            LoginUserContext principal = new LoginUserContext(userId, username, roleCodes, permissionCodes);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            // 后续 @PreAuthorize 和 SecurityUtils 都从这个上下文读取当前用户及其权限。
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            // 无效、篡改或过期的 accessToken 不建立登录态；后续由安全配置返回未认证响应。
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
        // Redis 中以 JSON 数组保存权限码，反序列化后直接转换为 Spring Security authority。
        return objectMapper.readValue(permissionJson, new TypeReference<List<String>>() {
        });
    }
}
