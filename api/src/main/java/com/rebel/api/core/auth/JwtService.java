package com.rebel.api.core.auth;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class JwtService {
	
	private final String secretKey;
	
	private final int jwtExpiration;
	
	public JwtService(String secretKey, int jwtExpiration) {
		super();
		this.secretKey = secretKey;
		this.jwtExpiration = jwtExpiration;
	}
	
	public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

//	public JwtDecoder jwtDecoder() {
//
//        SecretKey key = new SecretKeySpec(
//                secretKey.getBytes(StandardCharsets.UTF_8),
//                "HmacSHA256"
//        );
//
//        return NimbusJwtDecoder
//                .withSecretKey(key)
//                .build();
//    }
	
	public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    public String generateToken(String username) {
        return generateToken(new HashMap<>(), username);
    }

    public String generateToken(Map<String, Object> extraClaims, String username) {
        return buildToken(extraClaims, username, jwtExpiration);
    }
    
    public String generateToken(Map<String, Object> extraClaims) {
    	String email = extraClaims.get("email").toString();
        return generateToken(extraClaims, email);
    }
    
    public String generateToken(UserDetails user) {
    	
    	// Build JWT claims
        Map<String, Object> claims = new HashMap<>();

        claims.put(
                "roles",
                user.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList());
        
        return buildToken(claims, user.getUsername(), jwtExpiration);
    }

    public int getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            String username,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String username) {
        return (username.equals(extractUsername(token))) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
