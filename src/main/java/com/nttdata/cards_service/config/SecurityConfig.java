package com.nttdata.cards_service.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityWebFilterChain security(ServerHttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())
        .authorizeExchange(ex -> ex
            .pathMatchers("/actuator/**").permitAll()
            .anyExchange().hasAuthority("SCOPE_Partners"))
        .oauth2ResourceServer(oauth -> oauth
            .jwt(jwt -> jwt.jwtAuthenticationConverter(scpAndRoles())))
        .build();
  }

  @Bean
  public ReactiveJwtAuthenticationConverterAdapter scpAndRoles() {
    JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
    conv.setJwtGrantedAuthoritiesConverter(jwt -> {
      java.util.Collection<GrantedAuthority> out = new java.util.ArrayList<>();

      Object scope = jwt.getClaims().get("scope");
      if (scope instanceof String) {
        String s = (String) scope;
        for (String it : s.split(" ")) {
          if (!it.isEmpty()) out.add(new SimpleGrantedAuthority("SCOPE_" + it));
        }
      }

      Object scp = jwt.getClaims().get("scp");
      if (scp instanceof java.util.Collection) {
        for (Object v : (java.util.Collection<?>) scp) {
          if (v != null) out.add(new SimpleGrantedAuthority("SCOPE_" + String.valueOf(v)));
        }
      }

      java.util.Map<?, ?> realm = (java.util.Map<?, ?>) jwt.getClaims().get("realm_access");
      if (realm == null) realm = java.util.Collections.emptyMap();
      java.util.Collection<?> roles = (java.util.Collection<?>) realm.get("roles");
      if (roles == null) roles = java.util.Collections.emptyList();
      for (Object r : roles) {
        if (r != null) out.add(new SimpleGrantedAuthority("SCOPE_" + String.valueOf(r)));
      }

      return out;
    });
    return new ReactiveJwtAuthenticationConverterAdapter(conv);
  }
}