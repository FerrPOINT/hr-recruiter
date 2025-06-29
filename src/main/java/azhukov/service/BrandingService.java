package azhukov.service;

import azhukov.entity.Branding;
import azhukov.mapper.BrandingMapper;
import azhukov.repository.BrandingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Сервис для работы с настройками брендинга */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BrandingService {

  private final BrandingRepository brandingRepository;
  private final BrandingMapper brandingMapper;

  /** Получить настройки брендинга */
  @Transactional(readOnly = true)
  public azhukov.model.Branding getBranding() {
    log.debug("Получение настроек брендинга");

    Branding branding =
        brandingRepository
            .findFirstByOrderByIdAsc()
            .orElseGet(
                () -> {
                  log.info("Настройки брендинга не найдены, создаем по умолчанию");
                  Branding defaultBranding = new Branding();
                  defaultBranding.setCompanyName("HR Recruiter");
                  return brandingRepository.save(defaultBranding);
                });

    return brandingMapper.toDto(branding);
  }

  /** Обновить настройки брендинга */
  public azhukov.model.Branding updateBranding(azhukov.model.BrandingUpdateRequest request) {
    log.debug("Обновление настроек брендинга: {}", request);

    Branding branding =
        brandingRepository
            .findFirstByOrderByIdAsc()
            .orElseGet(
                () -> {
                  log.info("Настройки брендинга не найдены, создаем новые");
                  return new Branding();
                });

    brandingMapper.updateEntity(branding, request);
    Branding savedBranding = brandingRepository.save(branding);

    log.info("Настройки брендинга обновлены с ID: {}", savedBranding.getId());
    return brandingMapper.toDto(savedBranding);
  }
}
