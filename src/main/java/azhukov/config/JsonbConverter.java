package azhukov.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.util.StringUtils;

/**
 * Конвертер для работы с JSONB полями в H2. В H2 JSONB не поддерживается, поэтому используем TEXT.
 * В PostgreSQL будет использоваться нативный JSONB.
 */
@Converter
public class JsonbConverter implements AttributeConverter<String, String> {

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute;
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (!StringUtils.hasText(dbData)) {
      return null;
    }
    return dbData;
  }
}
