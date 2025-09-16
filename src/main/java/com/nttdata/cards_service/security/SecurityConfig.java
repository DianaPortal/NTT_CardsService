package com.nttdata.cards_service.security;

import lombok.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.*;
import org.springframework.security.config.annotation.web.reactive.*;
import org.springframework.security.config.web.server.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.server.*;
import org.springframework.security.web.server.authentication.*;
import org.springframework.security.web.server.context.*;
import org.springframework.security.web.server.util.matcher.*;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

  // Rutas públicas -no requieren autenticación ni autorización JWT
  private static final String[] PUBLIC_PATHS = new String[]{
      "/auth/**",
      "/actuator/health",
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html"
  };
  private final JwtService jwtService;

  //  Usuarios en memoria (para pruebas)
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public MapReactiveUserDetailsService reactiveUserDetailsService(PasswordEncoder encoder) {
    UserDetails user = User.builder()
        .username("user")
        .password(encoder.encode("password"))
        .roles("USER")
        .build();

    UserDetails admin = User.builder()
        .username("admin")
        .password(encoder.encode("admin123"))
        .roles("ADMIN")
        .build();

    return new MapReactiveUserDetailsService(user, admin);
  }

  // ===== Managers de autenticación =====

  // Manager para login con usuario/clave
  @Bean(name = "loginAuthenticationManager")
  public ReactiveAuthenticationManager loginAuthenticationManager(
      MapReactiveUserDetailsService uds,
      PasswordEncoder encoder
  ) {
    UserDetailsRepositoryReactiveAuthenticationManager m =
        new UserDetailsRepositoryReactiveAuthenticationManager(uds);
    m.setPasswordEncoder(encoder);
    return m;
  }

  // Manager para validar JWT en cada request protegida
  @Bean
  @Primary
  public ReactiveAuthenticationManager jwtAuthenticationManager() {
    return new JwtReactiveAuthenticationManager(jwtService);
  }

  // Extraer el token Bearer de la cabecera Authorization - Convertidor
  // que usará el filtro de autenticación
  @Bean
  public BearerTokenServerAuthenticationConverter bearerTokenConverter() {
    return new BearerTokenServerAuthenticationConverter();
  }

  // Validar el token JWT en las requests protegidas - Filtro
  @Bean
  public AuthenticationWebFilter jwtAuthWebFilter(
      @Qualifier("jwtAuthenticationManager") ReactiveAuthenticationManager jwtAuthManager,
      BearerTokenServerAuthenticationConverter bearerTokenConverter
  ) {
    // Filtro de autenticación JWT
    AuthenticationWebFilter filter = new AuthenticationWebFilter(jwtAuthManager);
    // Extraer el token Bearer de la cabecera Authorization
    filter.setServerAuthenticationConverter(bearerTokenConverter);

    // Excluir explícitamente las rutas públicas
    ServerWebExchangeMatcher protectedMatcher =
        new NegatedServerWebExchangeMatcher(
            ServerWebExchangeMatchers.pathMatchers(PUBLIC_PATHS)
        );
    //Aplicar solo a las rutas protegidas
    filter.setRequiresAuthenticationMatcher(protectedMatcher);
    filter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());

    return filter;
  }

  // Cadena de filtros de seguridad
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      AuthenticationWebFilter jwtAuthWebFilter
  ) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .authorizeExchange(ex -> ex
            .pathMatchers(PUBLIC_PATHS).permitAll()
            .anyExchange().authenticated()
        )
        .addFilterAt(jwtAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .exceptionHandling(e -> e
            .authenticationEntryPoint((exchange, ex) -> {
              exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); // 401
              return exchange.getResponse().setComplete();
            })
            .accessDeniedHandler((exchange, denied) -> {
              exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN); // 403
              return exchange.getResponse().setComplete();
            })
        )
        .build();
  }
}
