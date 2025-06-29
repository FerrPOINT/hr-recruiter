package azhukov.config;

import azhukov.model.PositionStatusEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PositionStatusEnumConverter implements Converter<String, PositionStatusEnum> {

  @Override
  public PositionStatusEnum convert(String source) {
    if (source == null || source.trim().isEmpty()) {
      return null;
    }

    try {
      return PositionStatusEnum.fromValue(source);
    } catch (IllegalArgumentException e) {
      // Если значение не найдено, возвращаем null
      return null;
    }
  }
}
