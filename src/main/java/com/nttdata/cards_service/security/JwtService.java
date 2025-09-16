package com.nttdata.cards_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.*;
import io.jsonwebtoken.security.*;
import lombok.*;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.*;

import javax.crypto.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

@Component
@RequiredArgsConstructor
public class JwtService {

  private final JwtProperties props;

  private SecretKey key() {
    byte[] bytes = Decoders.BASE64.decode(props.getSecret());
    return Keys.hmacShaKeyFor(bytes);
  }

  public String generateToken(UserDetails user) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.getExpiration());

    List<String> roles = user.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());

    return Jwts.builder()
        .header().type(Header.JWT_TYPE).and()
        .issuer(props.getIssuer())
        .subject(user.getUsername())
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .claim("roles", roles)
        .signWith(key(), Jwts.SIG.HS256)
        .compact();
  }

  public Jws<Claims> parse(String token) {
    return Jwts.parser()
        .verifyWith(key())
        .build()
        .parseSignedClaims(token);
  }

  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  public String getUsername(String token) {
    return parse(token).getPayload().getSubject();
  }

  @SuppressWarnings("unchecked")
  public List<String> getRoles(String token) {
    Object v = parse(token).getPayload().get("roles");
    return (v instanceof List) ? (List<String>) v : List.of();
  }
}