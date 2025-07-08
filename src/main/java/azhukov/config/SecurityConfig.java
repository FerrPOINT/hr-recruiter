package azhukov.config;

import azhukov.exception.RestAccessDeniedHandler;
import azhukov.exception.RestAuthenticationEntryPoint;
import azhukov.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

/** Конфигурация безопасности приложения. Настраивает аутентификацию, авторизацию и CORS. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

  private final ApplicationProperties applicationProperties;
  private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
  private final RestAccessDeniedHandler restAccessDeniedHandler;
  private final JwtService jwtService;
  private final JwtFilter jwtFilter;

  /** Настройка цепочки фильтров безопасности */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Отключаем CSRF для REST API
        .csrf(AbstractHttpConfigurer::disable)

        // Настройка CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // Настройка сессий
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Настройка авторизации запросов
        .authorizeHttpRequests(
            authz ->
                authz
                    // Публичные эндпоинты
                    .requestMatchers("/auth/**")
                    .permitAll()
                    .requestMatchers("/candidates/auth")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/h2-console/**")
                    .permitAll()

                    // Все API эндпоинты требуют аутентификации
                    .requestMatchers("/**")
                    .authenticated()

                    // Все остальные запросы требуют аутентификации
                    .anyRequest()
                    .authenticated())

        // Настройка HTTP Basic аутентификации
        .httpBasic(basic -> {})

        // Настройка заголовков безопасности
        .headers(
            headers ->
                headers
                    .frameOptions(frame -> frame.disable()) // Для H2 консоли
                    .contentTypeOptions(contentType -> {})
                    .httpStrictTransportSecurity(
                        hstsConfig -> hstsConfig.maxAgeInSeconds(31536000)))

        // Настройка exception handling для единого JSON-ответа
        .exceptionHandling(
            eh ->
                eh.authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler));

    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /** Настройка CORS */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Разрешенные источники
    configuration.setAllowedOriginPatterns(applicationProperties.getCors().getAllowedOrigins());

    // Разрешенные методы
    configuration.setAllowedMethods(applicationProperties.getCors().getAllowedMethods());

    // Разрешенные заголовки
    configuration.setAllowedHeaders(applicationProperties.getCors().getAllowedHeaders());

    // Разрешить учетные данные
    configuration.setAllowCredentials(applicationProperties.getCors().isAllowCredentials());

    // Максимальный возраст предварительного запроса
    configuration.setMaxAge(3600L);

    // Дополнительные заголовки для multipart запросов
    configuration.addAllowedHeader("Content-Type");
    configuration.addAllowedHeader("Authorization");
    configuration.addAllowedHeader("X-Requested-With");
    configuration.addAllowedHeader("Accept");
    configuration.addAllowedHeader("Origin");
    configuration.addAllowedHeader("Access-Control-Request-Method");
    configuration.addAllowedHeader("Access-Control-Request-Headers");

    // Настройка для всех путей
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  /** Кодировщик паролей */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /** Менеджер аутентификации */
  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
}

@Component
@RequiredArgsConstructor
class JwtFilter extends OncePerRequestFilter {
  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);

      if (jwtService.validateToken(token)) {
        String username = jwtService.extractUsername(token);
        if (username != null) {
          List<String> authorities = new ArrayList<>();
          if (jwtService.isAdminToken(token)) {
            authorities.add("ROLE_ADMIN");
          } else if (jwtService.isCandidateToken(token)) {
            authorities.add("ROLE_CANDIDATE");
          }

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  username,
                  null,
                  authorities.stream()
                      .map(authority -> new SimpleGrantedAuthority(authority))
                      .collect(Collectors.toList()));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
