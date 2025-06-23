package azhukov.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import azhukov.entity.Position;
import azhukov.model.PositionCreateRequest;
import azhukov.model.PositionStatusEnum;
import azhukov.model.PositionUpdateRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class PositionMapperTest {
  private final PositionMapperImpl mapper = new PositionMapperImpl();

  @Test
  void toDto_and_toEntity_basicMapping() {
    Position entity =
        Position.builder()
            .id(1L)
            .title("Java Dev")
            .company("Acme")
            .description("desc")
            .status(Position.Status.ACTIVE)
            .build();
    var dto = mapper.toDto(entity);
    assertThat(dto).isNotNull();
    assertThat(dto.getTitle()).isEqualTo("Java Dev");
    assertThat(dto.getCompany()).isEqualTo("Acme");
    assertThat(dto.getStatus()).isEqualTo(PositionStatusEnum.ACTIVE);

    PositionCreateRequest req =
        new PositionCreateRequest()
            .title("QA")
            .status(PositionStatusEnum.PAUSED)
            .description("test position");
    Position entity2 = mapper.toEntity(req);
    assertThat(entity2).isNotNull();
    assertThat(entity2.getTitle()).isEqualTo("QA");
    assertThat(entity2.getDescription()).isEqualTo("test position");
  }

  @Test
  void updateEntityFromRequest_shouldUpdateFields() {
    Position entity =
        Position.builder().title("Old").company("OldCo").status(Position.Status.PAUSED).build();
    PositionUpdateRequest req =
        new PositionUpdateRequest()
            .title("New")
            .status(PositionStatusEnum.ACTIVE)
            .description("updated");
    mapper.updateEntityFromRequest(req, entity);
    assertThat(entity.getTitle()).isEqualTo("New");
    assertThat(entity.getDescription()).isEqualTo("updated");
  }

  @Test
  void toDto_shouldReturnNullOnNull() {
    assertThat(mapper.toDto(null)).isNull();
  }

  @Test
  void toDtoList_shouldMapList() {
    Position e1 = Position.builder().title("A").company("B").status(Position.Status.ACTIVE).build();
    Position e2 = Position.builder().title("C").company("D").status(Position.Status.PAUSED).build();
    var dtos = mapper.toDtoList(List.of(e1, e2));
    assertThat(dtos).hasSize(2);
    assertThat(dtos.get(0).getTitle()).isEqualTo("A");
    assertThat(dtos.get(1).getTitle()).isEqualTo("C");
  }
}
