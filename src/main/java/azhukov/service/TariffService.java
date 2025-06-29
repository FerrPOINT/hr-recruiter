package azhukov.service;

import azhukov.entity.Tariff;
import azhukov.entity.UserEntity;
import azhukov.exception.ResourceNotFoundException;
import azhukov.mapper.TariffMapper;
import azhukov.repository.PositionRepository;
import azhukov.repository.TariffRepository;
import azhukov.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Сервис для работы с тарифными планами */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TariffService {

  private final TariffRepository tariffRepository;
  private final TariffMapper tariffMapper;
  private final UserRepository userRepository;
  private final PositionRepository positionRepository;

  /** Получить все активные тарифы */
  @Transactional(readOnly = true)
  public List<azhukov.model.Tariff> getAllActiveTariffs() {
    log.debug("Получение всех активных тарифов");
    List<Tariff> tariffs = tariffRepository.findByIsActiveTrueOrderByPriceAsc();
    return tariffs.stream().map(tariffMapper::toDto).collect(Collectors.toList());
  }

  /** Получить тариф по ID */
  @Transactional(readOnly = true)
  public azhukov.model.Tariff getTariffById(Long id) {
    log.debug("Получение тарифа по ID: {}", id);
    Tariff tariff =
        tariffRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Тариф с ID " + id + " не найден"));
    return tariffMapper.toDto(tariff);
  }

  /** Создать новый тариф */
  public azhukov.model.Tariff createTariff(azhukov.model.TariffCreateRequest request) {
    log.debug("Создание нового тарифа: {}", request.getName());

    // Проверяем уникальность названия
    if (tariffRepository.existsByNameIgnoreCase(request.getName())) {
      throw new IllegalArgumentException(
          "Тариф с названием '" + request.getName() + "' уже существует");
    }

    Tariff tariff = tariffMapper.toEntity(request);
    Tariff savedTariff = tariffRepository.save(tariff);

    log.info("Создан новый тариф с ID: {}", savedTariff.getId());
    return tariffMapper.toDto(savedTariff);
  }

  /** Обновить тариф */
  public azhukov.model.Tariff updateTariff(Long id, azhukov.model.TariffUpdateRequest request) {
    log.debug("Обновление тарифа с ID: {}", id);

    Tariff tariff =
        tariffRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Тариф с ID " + id + " не найден"));

    // Проверяем уникальность названия (если изменилось)
    if (!tariff.getName().equalsIgnoreCase(request.getName())
        && tariffRepository.existsByNameIgnoreCase(request.getName())) {
      throw new IllegalArgumentException(
          "Тариф с названием '" + request.getName() + "' уже существует");
    }

    tariffMapper.updateEntity(tariff, request);
    Tariff updatedTariff = tariffRepository.save(tariff);

    log.info("Тариф с ID {} обновлен", updatedTariff.getId());
    return tariffMapper.toDto(updatedTariff);
  }

  /** Удалить тариф */
  public void deleteTariff(Long id) {
    log.debug("Удаление тарифа с ID: {}", id);

    Tariff tariff =
        tariffRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Тариф с ID " + id + " не найден"));

    tariffRepository.delete(tariff);
    log.info("Тариф с ID {} удален", id);
  }

  /** Получить информацию о тарифе пользователя */
  @Transactional(readOnly = true)
  public azhukov.model.GetTariffInfo200Response getUserTariffInfo(String userEmail) {
    log.debug("Получение информации о тарифе пользователя: {}", userEmail);

    UserEntity user =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Пользователь с email " + userEmail + " не найден"));

    // Для демонстрации возвращаем информацию о бесплатном тарифе
    // В реальном приложении здесь была бы логика получения подписки пользователя
    azhukov.model.GetTariffInfo200Response tariffInfo =
        new azhukov.model.GetTariffInfo200Response();

    // Подсчитываем использованные ресурсы
    long usedPositions = positionRepository.countByCreatedBy(user);

    // Для демонстрации устанавливаем примерные значения
    int usedInterviews = 25;
    int maxInterviews = 100;
    int interviewsLeft = maxInterviews - usedInterviews;

    tariffInfo.setInterviewsLeft((long) interviewsLeft);
    tariffInfo.setUntil(LocalDateTime.now().plusDays(335).toString());

    return tariffInfo;
  }

  /** Получить самый дешевый активный тариф */
  @Transactional(readOnly = true)
  public azhukov.model.Tariff getCheapestTariff() {
    log.debug("Получение самого дешевого тарифа");
    Tariff tariff =
        tariffRepository
            .findCheapestActiveTariff()
            .orElseThrow(() -> new ResourceNotFoundException("Активные тарифы не найдены"));
    return tariffMapper.toDto(tariff);
  }

  /** Получить тарифы с AI функциями */
  @Transactional(readOnly = true)
  public List<azhukov.model.Tariff> getTariffsWithAiFeatures() {
    log.debug("Получение тарифов с AI функциями");
    List<Tariff> tariffs =
        tariffRepository.findByIsActiveTrueAndAiFeaturesEnabledTrueOrderByPriceAsc();
    return tariffs.stream().map(tariffMapper::toDto).collect(Collectors.toList());
  }
}
