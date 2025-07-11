package azhukov.controller;

import azhukov.api.InterviewsApi;
import azhukov.model.*;
import azhukov.service.ElevenLabsAgentService;
import azhukov.service.InterviewService;
import azhukov.service.PositionService;
import azhukov.service.QuestionService;
import azhukov.service.VoiceInterviewService;
import azhukov.util.PaginationUtils;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для управления собеседованиями. Реализует интерфейс InterviewsApi, сгенерированный из
 * OpenAPI спецификации.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class InterviewsApiController extends BaseController implements InterviewsApi {

  private final InterviewService interviewService;
  private final VoiceInterviewService voiceInterviewService;
  private final ElevenLabsAgentService elevenLabsAgentService;
  private final PositionService positionService;
  private final QuestionService questionService;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<azhukov.model.Interview> createInterviewFromCandidate(Long candidateId) {
    log.info("Creating interview for candidate: {}", candidateId);
    azhukov.model.Interview interviewDto =
        interviewService.createInterviewFromCandidate(candidateId);
    return ResponseEntity.status(HttpStatus.CREATED).body(interviewDto);
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<GetInterview200Response> getInterview(Long id) {
    log.info("Getting interview information: {}", id);
    GetInterview200Response response = interviewService.getInterviewDetails(id);
    return ResponseEntity.ok(response);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PaginatedResponse> listInterviews(
      Optional<Long> positionId,
      Optional<Long> candidateId,
      Optional<Long> page,
      Optional<Long> size,
      Optional<String> sort) {
    log.info(
        "Listing interviews with positionId: {}, candidateId: {}, page: {}, size: {}, sort: {}",
        positionId.orElse(null),
        candidateId.orElse(null),
        page.orElse(0L),
        size.orElse(20L),
        sort.orElse("createdAt"));

    Pageable pageable = PaginationUtils.createPageableFromOptional(page, size);

    Page<azhukov.model.Interview> interviews =
        interviewService.getInterviewsPage(
            positionId.orElse(null), candidateId.orElse(null), pageable);

    PaginatedResponse response = new PaginatedResponse();
    PaginationUtils.fillPaginationFields(interviews, response);

    return ResponseEntity.ok(response);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<azhukov.model.Interview>> listPositionInterviews(Long positionId) {
    log.info("Listing interviews for position: {}", positionId);
    List<azhukov.model.Interview> interviewDtos =
        interviewService.getInterviewsByPosition(positionId);
    return ResponseEntity.ok(interviewDtos);
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<azhukov.model.Interview> finishInterview(Long id) {
    log.info("Manually finishing interview: {}", id);
    azhukov.entity.Interview finishedInterview = interviewService.finishInterview(id);
    azhukov.model.Interview interviewDto =
        interviewService.getInterviewMapper().toDto(finishedInterview);
    return ResponseEntity.ok(interviewDto);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<GetChecklist200ResponseInner>> getChecklist() {
    log.info("Getting interview checklist");
    List<GetChecklist200ResponseInner> checklist = interviewService.getInterviewChecklist();
    return ResponseEntity.ok(checklist);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<GetInviteInfo200Response> getInviteInfo() {
    log.info("Getting invite information");
    GetInviteInfo200Response inviteInfo = interviewService.getInviteInfo();
    return ResponseEntity.ok(inviteInfo);
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<InterviewStartResponse> startInterview(
      Long id, Optional<InterviewStartRequest> interviewStartRequest) {
    log.info(
        "Starting interview: {} with voice mode: {}, includeCandidateData: {}",
        id,
        interviewStartRequest.map(req -> req.getVoiceMode()).orElse(false),
        interviewStartRequest.map(req -> req.getIncludeCandidateData()).orElse(true));

    InterviewStartResponse response = new InterviewStartResponse();
    response.setInterviewId(id);

    // Запускаем интервью
    interviewService.startInterview(id);

    // Обрабатываем голосовой режим
    if (interviewStartRequest.isPresent() && interviewStartRequest.get().getVoiceMode()) {
      // Создаем агента ElevenLabs если нужно
      String agentId = null;
      if (interviewStartRequest.get().getAutoCreateAgent() != null
          && interviewStartRequest.get().getAutoCreateAgent()) {
        try {
          var agent = elevenLabsAgentService.createAgentForInterview(id);
          agentId = agent.getElevenLabsAgentId();
          log.info("Created ElevenLabs agent: {} for interview: {}", agentId, id);
        } catch (Exception e) {
          log.error("Failed to create ElevenLabs agent for interview: {}", id, e);
          // Продолжаем без агента, используем дефолтный
        }
      }

      // Создаем голосовую сессию
      VoiceSessionCreateRequest voiceRequest = new VoiceSessionCreateRequest();
      voiceRequest.setVoiceMode(true);
      voiceRequest.setAgentConfig(interviewStartRequest.get().getAgentConfig());
      voiceRequest.setVoiceSettings(interviewStartRequest.get().getVoiceSettings());
      voiceRequest.setAutoCreateAgent(interviewStartRequest.get().getAutoCreateAgent());

      VoiceSessionResponse voiceSession =
          voiceInterviewService.createVoiceSession(id, voiceRequest);

      response.setAgentId(agentId != null ? agentId : voiceSession.getAgentId());
      response.setSessionId(voiceSession.getSessionId());
      response.setStatus(InterviewStartStatusEnum.AGENT_CREATED);
      response.setMessage("Voice session created successfully");
      response.setWebhookUrl(
          voiceSession.getWebhookUrl() != null
              ? voiceSession.getWebhookUrl()
              : "/api/v1/webhooks/elevenlabs/events");
    } else {
      response.setStatus(InterviewStartStatusEnum.STARTED);
      response.setMessage("Interview started successfully");
    }

    // Добавляем данные для кандидата если запрошено
    if (interviewStartRequest.isPresent()
        && interviewStartRequest.get().getIncludeCandidateData() != null
        && interviewStartRequest.get().getIncludeCandidateData()) {
      InterviewCandidateData candidateData = buildCandidateData(id);
      response.setCandidateData(candidateData);
    }

    return ResponseEntity.ok(response);
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMIN', 'CANDIDATE')")
  public ResponseEntity<azhukov.model.Interview> submitInterviewAnswer(
      Long id, InterviewAnswerCreateRequest interviewAnswerCreateRequest) {
    log.info("Submitting interview answer for interview: {}", id);
    azhukov.entity.Interview updatedInterview =
        interviewService.submitInterviewAnswer(id, interviewAnswerCreateRequest);
    azhukov.model.Interview interviewDto =
        interviewService.getInterviewMapper().toDto(updatedInterview);
    return ResponseEntity.ok(interviewDto);
  }

  /** Строит данные для кандидата при старте интервью */
  private InterviewCandidateData buildCandidateData(Long interviewId) {
    // Получаем интервью
    azhukov.entity.Interview interview = interviewService.findById(interviewId).orElseThrow();

    // Получаем позицию
    azhukov.entity.Position position =
        positionService.findById(interview.getPosition().getId()).orElseThrow();

    // Получаем вопросы
    List<azhukov.entity.Question> questions =
        questionService.getPositionQuestions(position.getId());

    InterviewCandidateData data = new InterviewCandidateData();

    // Базовые данные интервью
    InterviewCandidateDataInterview interviewData = new InterviewCandidateDataInterview();
    interviewData.setId(interview.getId());
    interviewData.setStatus(InterviewStatusEnum.fromValue(interview.getStatus().name()));
    interviewData.setCreatedAt(interview.getCreatedAt().atOffset(java.time.ZoneOffset.UTC));
    interviewData.setStartedAt(
        interview.getStartedAt() != null
            ? interview.getStartedAt().atOffset(java.time.ZoneOffset.UTC)
            : null);
    interviewData.setFinishedAt(
        interview.getFinishedAt() != null
            ? interview.getFinishedAt().atOffset(java.time.ZoneOffset.UTC)
            : null);
    data.setInterview(interviewData);

    // Настройки из позиции
    InterviewCandidateDataSettings settings = new InterviewCandidateDataSettings();
    settings.setAnswerTime(
        position.getAnswerTime() != null ? position.getAnswerTime().longValue() : 120L);
    settings.setLanguage(position.getLanguage() != null ? position.getLanguage() : "Русский");
    settings.setSaveAudio(position.getSaveAudio() != null ? position.getSaveAudio() : true);
    settings.setSaveVideo(position.getSaveVideo() != null ? position.getSaveVideo() : false);
    settings.setRandomOrder(position.getRandomOrder() != null ? position.getRandomOrder() : false);
    settings.setMinScore(
        position.getMinScore() != null ? position.getMinScore().floatValue() : 70.0f);
    data.setSettings(settings);

    // Вопросы
    List<InterviewCandidateDataQuestionsInner> questionsData =
        questions.stream()
            .map(
                q -> {
                  InterviewCandidateDataQuestionsInner questionData =
                      new InterviewCandidateDataQuestionsInner();
                  questionData.setId(q.getId());
                  questionData.setText(q.getText());
                  questionData.setType(QuestionTypeEnum.fromValue(q.getType().name()));
                  questionData.setOrder(q.getOrder() != null ? q.getOrder().longValue() : 0L);
                  questionData.setIsRequired(true); // По умолчанию все вопросы обязательные
                  return questionData;
                })
            .toList();
    data.setQuestions(questionsData);

    // Информация о позиции
    InterviewCandidateDataPosition positionData = new InterviewCandidateDataPosition();
    positionData.setTitle(position.getTitle());
    positionData.setLevel(PositionLevelEnum.fromValue(position.getLevel().name()));
    data.setPosition(positionData);

    // Прогресс
    InterviewCandidateDataProgress progress = new InterviewCandidateDataProgress();
    progress.setCurrentQuestion(0L);
    progress.setTotalQuestions((long) questions.size());
    progress.setAnsweredQuestions(0L);
    progress.setRemainingTime((long) (questions.size() * settings.getAnswerTime().intValue()));
    data.setProgress(progress);

    // Чек-лист
    List<InterviewCandidateDataChecklistInner> checklist =
        List.of(
            createChecklistItem("Проверьте работу микрофона", false),
            createChecklistItem("Убедитесь в стабильном интернет-соединении", false),
            createChecklistItem("Подготовьте тихое место для интервью", false),
            createChecklistItem("Закройте лишние приложения", false));
    data.setChecklist(checklist);

    // Информация для приглашения
    InterviewCandidateDataInviteInfo inviteInfo = new InterviewCandidateDataInviteInfo();
    inviteInfo.setLanguage(settings.getLanguage());
    inviteInfo.setQuestionsCount((long) questions.size());
    inviteInfo.setEstimatedDuration(
        (long)
            (questions.size() * (settings.getAnswerTime().intValue() / 60)
                + 5)); // +5 минут на подготовку
    data.setInviteInfo(inviteInfo);

    return data;
  }

  private InterviewCandidateDataChecklistInner createChecklistItem(String text, boolean completed) {
    InterviewCandidateDataChecklistInner item = new InterviewCandidateDataChecklistInner();
    item.setText(text);
    item.setCompleted(completed);
    return item;
  }
}
