package com.nttdata.cards_service.adapter.auth;


import com.nttdata.cards_service.security.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final ReactiveAuthenticationManager loginAuthenticationManager;
  private final MapReactiveUserDetailsService uds;
  private final JwtService jwtService;

  public AuthController(
      @org.springframework.beans.factory.annotation.Qualifier("loginAuthenticationManager")
      ReactiveAuthenticationManager loginAuthenticationManager,
      MapReactiveUserDetailsService uds,
      JwtService jwtService
  ) {
    this.loginAuthenticationManager = loginAuthenticationManager;
    this.uds = uds;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  public reactor.core.publisher.Mono<org.springframework.http.ResponseEntity<com.nttdata.cards_service.model.auth.LoginResponse>> login(
      @RequestBody com.nttdata.cards_service.model.auth.LoginRequest req) {

    org.springframework.security.authentication.UsernamePasswordAuthenticationToken authReq =
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            req.getUsername(), req.getPassword());

    return loginAuthenticationManager.authenticate(authReq)
        .map(org.springframework.security.core.Authentication::getPrincipal)
        .cast(org.springframework.security.core.userdetails.UserDetails.class)
        .map(u -> new com.nttdata.cards_service.model.auth.LoginResponse()
            .setTokenType("Bearer")
            .setToken(jwtService.generateToken(u)))
        .map(org.springframework.http.ResponseEntity::ok);
  }
}
