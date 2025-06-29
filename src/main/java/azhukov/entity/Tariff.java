package azhukov.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Сущность для хранения тарифных планов */
@Entity
@Table(name = "tariffs")
@Data
@NoArgsConstructor
public class Tariff {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Название тарифа обязательно")
  @Column(nullable = false, length = 100)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @ElementCollection
  @CollectionTable(name = "tariff_features", joinColumns = @JoinColumn(name = "tariff_id"))
  @Column(name = "feature", length = 200)
  private List<String> features = new ArrayList<>();

  @NotNull(message = "Цена тарифа обязательна")
  @Positive(message = "Цена должна быть положительной")
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "is_active", nullable = false)
  @NotNull(message = "Статус активности обязателен")
  private Boolean isActive = true;

  @Column(name = "max_positions")
  private Integer maxPositions;

  @Column(name = "max_candidates")
  private Integer maxCandidates;

  @Column(name = "max_interviews")
  private Integer maxInterviews;

  @Column(name = "ai_features_enabled")
  private Boolean aiFeaturesEnabled = false;

  @Column(name = "priority_support")
  private Boolean prioritySupport = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  public void prePersist() {
    if (name != null) {
      name = name.trim();
    }
    if (description != null) {
      description = description.trim();
    }
    if (isActive == null) {
      isActive = true;
    }
    if (aiFeaturesEnabled == null) {
      aiFeaturesEnabled = false;
    }
    if (prioritySupport == null) {
      prioritySupport = false;
    }
  }
}
