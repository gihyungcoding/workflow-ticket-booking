package com.example.ticket_booking.api.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * JSON 문자열만 받는 String 역직렬화기. 기본 동작은 숫자·불리언을 문자열로 조용히 바꿔 받는다(123 → "123") — 토큰처럼 값이 그대로 의미를 갖는 필드에서는
 * 이것이 다른 오류(401)로 가려져 요청 형식 오류(400)가 되지 않는다. 필요한 필드에만 붙인다.
 */
final class StrictStringDeserializer extends StdScalarDeserializer<String> {

  StrictStringDeserializer() {
    super(String.class);
  }

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) {
    if (parser.currentToken() == JsonToken.VALUE_STRING) {
      return parser.getString();
    }
    return (String) context.handleUnexpectedToken(String.class, parser);
  }
}
