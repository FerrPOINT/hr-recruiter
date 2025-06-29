package azhukov.controller;

import azhukov.api.SettingsApi;
import azhukov.model.GetTariffInfo200Response;
import azhukov.service.BrandingService;
import azhukov.service.TariffService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для настроек системы (брендинг, тарифы) по openapi. Реализует SettingsApi и делегирует
 * в соответствующие сервисы.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SettingsApiController implements SettingsApi {

  private final BrandingService brandingService;
  private final TariffService tariffService;

  // ---- Branding ----
  @Override
  public ResponseEntity<azhukov.model.Branding> getBranding() {
    log.debug("GET /branding - получение настроек брендинга");
    azhukov.model.Branding branding = brandingService.getBranding();
    return ResponseEntity.ok(branding);
  }

  @Override
  public ResponseEntity<azhukov.model.Branding> updateBranding(
      azhukov.model.BrandingUpdateRequest request) {
    log.debug("PUT /branding - обновление настроек брендинга");
    azhukov.model.Branding branding = brandingService.updateBranding(request);
    return ResponseEntity.ok(branding);
  }

  // ---- Tariffs ----
  @Override
  public ResponseEntity<List<azhukov.model.Tariff>> listTariffs() {
    log.debug("GET /tariffs - получение списка тарифов");
    List<azhukov.model.Tariff> tariffs = tariffService.getAllActiveTariffs();
    return ResponseEntity.ok(tariffs);
  }

  @Override
  public ResponseEntity<azhukov.model.Tariff> createTariff(
      azhukov.model.TariffCreateRequest request) {
    log.debug("POST /tariffs - создание тарифа");
    azhukov.model.Tariff tariff = tariffService.createTariff(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(tariff);
  }

  @Override
  public ResponseEntity<azhukov.model.Tariff> getTariff(Long id) {
    log.debug("GET /tariffs/{} - получение тарифа", id);
    azhukov.model.Tariff tariff = tariffService.getTariffById(id);
    return ResponseEntity.ok(tariff);
  }

  @Override
  public ResponseEntity<azhukov.model.Tariff> updateTariff(Long id, azhukov.model.Tariff tariff) {
    log.debug("PUT /tariffs/{} - обновление тарифа", id);
    azhukov.model.TariffUpdateRequest updateRequest = new azhukov.model.TariffUpdateRequest();
    updateRequest.setName(tariff.getName());
    updateRequest.setPrice(tariff.getPrice());
    updateRequest.setFeatures(tariff.getFeatures());
    updateRequest.setIsActive(tariff.getIsActive());
    azhukov.model.Tariff updated = tariffService.updateTariff(id, updateRequest);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<Void> deleteTariff(Long id) {
    log.debug("DELETE /tariffs/{} - удаление тарифа", id);
    tariffService.deleteTariff(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<GetTariffInfo200Response> getTariffInfo() {
    log.debug("GET /tariff/info - получение информации о тарифе пользователя");
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userEmail = authentication.getName();
    GetTariffInfo200Response info = tariffService.getUserTariffInfo(userEmail);
    return ResponseEntity.ok(info);
  }
}
