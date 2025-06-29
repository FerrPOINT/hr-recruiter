package azhukov.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Конфигурация безопасности приложения. Настраивает аутентификацию, авторизацию и CORS. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

  private final ApplicationProperties applicationProperties;

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
                    // Публичные эндпоинты (с учетом context-path /api/v1)
                    .requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/api/v1/actuator/**")
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
                        hstsConfig -> hstsConfig.maxAgeInSeconds(31536000)));

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
