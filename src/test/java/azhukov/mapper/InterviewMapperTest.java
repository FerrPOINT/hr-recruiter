package azhukov.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import azhukov.entity.Interview;
import azhukov.model.InterviewResultEnum;
import azhukov.model.InterviewStatusEnum;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InterviewMapperTest {
  @Autowired private InterviewMapper mapper;

  @Test
  void toDto_and_toEntity_basicMapping() {
    var candidate = new azhukov.entity.Candidate();
    candidate.setId(10L);
    var position = new azhukov.entity.Position();
    position.setId(20L);
    Interview entity =
        Interview.builder()
            .id(1L)
            .status(Interview.Status.NOT_STARTED)
            .result(Interview.Result.SUCCESSFUL)
            .candidate(candidate)
            .position(position)
            .answers(List.of())
            .build();
    var dto = mapper.toDto(entity);
    assertThat(dto).isNotNull();
    assertThat(dto.getStatus()).isEqualTo(InterviewStatusEnum.NOT_STARTED);
    assertThat(dto.getResult()).isEqualTo(InterviewResultEnum.SUCCESSFUL);
    assertThat(dto.getCandidateId()).isEqualTo(10L);
    assertThat(dto.getPositionId()).isEqualTo(20L);

    azhukov.model.Interview dto2 =
        new azhukov.model.Interview()
            .status(InterviewStatusEnum.IN_PROGRESS)
            .result(InterviewResultEnum.UNSUCCESSFUL);
    Interview entity2 = mapper.toEntity(dto2);
    assertThat(entity2).isNotNull();
    assertThat(entity2.getStatus()).isEqualTo(Interview.Status.IN_PROGRESS);
    assertThat(entity2.getResult()).isEqualTo(Interview.Result.UNSUCCESSFUL);
  }

  @Test
  void toDto_shouldReturnNullOnNull() {
    assertThat(mapper.toDto(null)).isNull();
  }

  @Test
  void toDtoList_shouldMapList() {
    var candidate = new azhukov.entity.Candidate();
    candidate.setId(1L);
    var position = new azhukov.entity.Position();
    position.setId(2L);
    Interview e1 =
        Interview.builder()
            .status(Interview.Status.NOT_STARTED)
            .candidate(candidate)
            .position(position)
            .answers(List.of())
            .build();
    Interview e2 =
        Interview.builder()
            .status(Interview.Status.FINISHED)
            .candidate(candidate)
            .position(position)
            .answers(List.of())
            .build();
    var dtos = mapper.toDtoList(List.of(e1, e2));
    assertThat(dtos).hasSize(2);
    assertThat(dtos.get(0).getStatus()).isEqualTo(InterviewStatusEnum.NOT_STARTED);
    assertThat(dtos.get(1).getStatus()).isEqualTo(InterviewStatusEnum.FINISHED);
  }
}
