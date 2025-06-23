package azhukov.service;

import azhukov.entity.UserEntity;
import azhukov.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для загрузки пользователей Spring Security. Реализует UserDetailsService для интеграции с
 * Spring Security.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /** Загружает пользователя по email для Spring Security */
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    log.debug("Loading user by email: {}", email);

    UserEntity user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException("Пользователь с email " + email + " не найден"));

    if (!user.isActive()) {
      throw new UsernameNotFoundException("Пользователь с email " + email + " неактивен");
    }

    log.debug("Loaded user: {} with roles: {}", email, user.getAuthorities());
    return user;
  }

  /** Загружает пользователя по ID */
  public UserDetails loadUserById(String id) throws UsernameNotFoundException {
    log.debug("Loading user by ID: {}", id);

    UserEntity user =
        userRepository
            .findById(id)
            .orElseThrow(
                () -> new UsernameNotFoundException("Пользователь с ID " + id + " не найден"));

    if (!user.isActive()) {
      throw new UsernameNotFoundException("Пользователь с ID " + id + " неактивен");
    }

    return user;
  }
}
