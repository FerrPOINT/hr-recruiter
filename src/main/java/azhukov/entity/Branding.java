package azhukov.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Сущность для хранения настроек брендинга компании */
@Entity
@Table(name = "branding")
@Data
@NoArgsConstructor
public class Branding {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Название компании обязательно")
  @Column(name = "company_name", nullable = false, length = 200)
  private String companyName;

  @Column(name = "logo_url")
  private String logoUrl;

  @Column(name = "primary_color", length = 7)
  private String primaryColor;

  @Column(name = "secondary_color", length = 7)
  private String secondaryColor;

  @Column(name = "email_signature", columnDefinition = "TEXT")
  private String emailSignature;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  public void prePersist() {
    if (companyName != null) {
      companyName = companyName.trim();
    }
    if (primaryColor != null) {
      primaryColor = primaryColor.trim();
    }
    if (secondaryColor != null) {
      secondaryColor = secondaryColor.trim();
    }
  }
}
