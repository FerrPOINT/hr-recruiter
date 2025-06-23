package azhukov.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Сущность пользователя системы. Реализует UserDetails для интеграции со Spring Security. */
@Entity
@Table(
    name = "users",
    indexes = {
      @Index(name = "idx_users_email", columnList = "email", unique = true),
      @Index(name = "idx_users_role", columnList = "role"),
      @Index(name = "idx_users_phone", columnList = "phone", unique = true)
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "password")
public class UserEntity implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Имя обязательно")
  @Column(nullable = false, length = 100)
  private String firstName;

  @NotBlank(message = "Фамилия обязательна")
  @Column(nullable = false, length = 100)
  private String lastName;

  @Email(message = "Некорректный email")
  @NotBlank(message = "Email обязателен")
  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @NotBlank(message = "Пароль обязателен")
  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull(message = "Роль обязательна")
  private Role role;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(length = 10)
  @Builder.Default
  private String language = "ru";

  @Column(name = "is_active")
  @Builder.Default
  private boolean active = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(length = 20, unique = true)
  private String phone;

  // Spring Security UserDetails implementation
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return active;
  }

  @Override
  public boolean isAccountNonLocked() {
    return active;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return active;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }

  /** Роли пользователей в системе */
  public enum Role {
    ADMIN, // Администратор - полный доступ
    RECRUITER, // Рекрутер - создание вакансий, управление кандидатами
    VIEWER // Наблюдатель - только просмотр данных
  }
}
