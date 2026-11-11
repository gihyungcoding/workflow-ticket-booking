package com.example.ticket_booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * 구역 묶음 하나 — 무대에서 가까운 묶음부터 보낸다. 문자열은 변환 없이 원문으로 검증한다(ADR-0019): 제어문자·짝 없는 서로게이트를 거부하고 앞뒤 공백은 그대로
 * 둔다. 숫자·불리언을 문자열로 받아 주지 않는다 — grades 의 1 이 등급 "1" 과 맞아 버린다. grades 가 sections 의 등급과 맞는지는 두 필드를 함께
 * 봐야 해 PerformanceService 가 판정한다.
 */
public record ZoneRequest(
    @NotBlank
        @Size(max = 30)
        @Pattern(regexp = ZoneRequest.NO_CONTROL, message = ZoneRequest.NO_CONTROL_MESSAGE)
        @JsonDeserialize(using = StrictStringDeserializer.class)
        String area,
    @Size(max = 60)
        @Pattern(regexp = ZoneRequest.NO_CONTROL, message = ZoneRequest.NO_CONTROL_MESSAGE)
        @Pattern(regexp = "(?s).*\\S.*", message = "note 를 주려면 공백이 아닌 글자가 있어야 합니다")
        @JsonDeserialize(using = StrictStringDeserializer.class)
        String note,
    @NotEmpty @JsonDeserialize(contentUsing = StrictStringDeserializer.class)
        List<@NotBlank String> grades) {

  static final String NO_CONTROL = "^[^\\p{Cc}\\p{Cs}]*$";
  static final String NO_CONTROL_MESSAGE = "제어 문자나 짝 없는 서로게이트를 포함할 수 없습니다";
}
