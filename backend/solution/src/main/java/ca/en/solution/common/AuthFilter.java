package ca.en.solution.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Rejects every request that does not carry exactly "Bearer <token>", before any route logic.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    private final byte[] validHeader;
    private final JsonMapper jsonMapper;

    public AuthFilter(@Value("${auth.token:superday-demo-token}") String token, JsonMapper jsonMapper) {
        this.validHeader = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        // A constant-time comparison, so the response time does not reveal how much of the token matched.
        if (header == null || !MessageDigest.isEqual(validHeader, header.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            jsonMapper.writeValue(response.getWriter(), new ApiError("unauthorized",
                    "Missing or invalid Authorization header. Expected: Bearer <token>."));
            return;
        }
        chain.doFilter(request, response);
    }

}
