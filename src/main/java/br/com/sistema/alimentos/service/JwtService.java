package br.com.sistema.alimentos.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey signingKey;

    // ====================================================
    // gerarToken - Gera um token JWT para o usuário autenticado
    // ====================================================
    public String gerarToken(UserDetails userDetails) {
        return gerarToken(new HashMap<>(), userDetails);
    }

    public String gerarToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ====================================================
    // validarToken - Verifica se o token é válido para o usuário
    // ====================================================
    public boolean validarToken(String token, UserDetails userDetails) {
        final String email = extrairEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpirado(token);
    }

    // ====================================================
    // extrairEmail - Extrai o e-mail (subject) do token JWT
    // ====================================================
    public String extrairEmail(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    private boolean isTokenExpirado(String token) {
        return extrairExpiracao(token).before(new Date());
    }

    private Date extrairExpiracao(String token) {
        return extrairClaim(token, Claims::getExpiration);
    }

    private <T> T extrairClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extrairTodosOsClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extrairTodosOsClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @PostConstruct
    private void init() {
        initSigningKey();
    }

    private void initSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Propriedade 'jwt.secret' não configurada");
        }

        byte[] keyBytes = deriveKeyBytes(secret.trim());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("Propriedade 'jwt.secret' inválida: chave deve ter no mínimo 32 bytes");
        }

        try {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Propriedade 'jwt.secret' inválida para HMAC", ex);
        }
    }

    private byte[] deriveKeyBytes(String rawSecret) {
        // Tenta Base64; se o resultado ficar curto, cai para texto puro UTF-8
        try {
            byte[] decoded = Decoders.BASE64.decode(rawSecret);
            if (decoded.length >= 32) {
                return decoded;
            }
            return rawSecret.getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return rawSecret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private SecretKey getSigningKey() {
        if (signingKey == null) {
            initSigningKey();
        }
        return signingKey;
    }
}