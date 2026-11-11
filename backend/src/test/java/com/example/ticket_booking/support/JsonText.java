package com.example.ticket_booking.support;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 요청 본문을 손으로 직렬화한다. 제어문자·서로게이트는 전부 {@code \\uXXXX} 로 적는다 — 직렬화기가 짝 없는 서로게이트를 거부하거나 바꾸면 시나리오가 보내려는
 * 값이 서버에 닿지 않는다. {@link Raw} 는 그대로 끼워 넣어 다른 JSON 타입(숫자·객체 등)을 보낸다.
 */
public final class JsonText {

  public record Raw(String json) {}

  private JsonText() {}

  public static Raw raw(String json) {
    return new Raw(json);
  }

  public static String of(Object value) {
    StringBuilder out = new StringBuilder();
    write(out, value);
    return out.toString();
  }

  private static void write(StringBuilder out, Object value) {
    if (value == null) {
      out.append("null");
    } else if (value instanceof Raw raw) {
      out.append(raw.json());
    } else if (value instanceof String text) {
      writeString(out, text);
    } else if (value instanceof Number || value instanceof Boolean) {
      out.append(value);
    } else if (value instanceof Map<?, ?> map) {
      out.append('{');
      Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet().iterator();
      while (entries.hasNext()) {
        Map.Entry<?, ?> entry = entries.next();
        writeString(out, String.valueOf(entry.getKey()));
        out.append(':');
        write(out, entry.getValue());
        if (entries.hasNext()) {
          out.append(',');
        }
      }
      out.append('}');
    } else if (value instanceof List<?> list) {
      out.append('[');
      for (int i = 0; i < list.size(); i++) {
        if (i > 0) {
          out.append(',');
        }
        write(out, list.get(i));
      }
      out.append(']');
    } else {
      throw new IllegalArgumentException("직렬화할 수 없는 값: " + value.getClass());
    }
  }

  private static void writeString(StringBuilder out, String text) {
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '"' || c == '\\') {
        out.append('\\').append(c);
      } else if (c < 0x20 || Character.isSurrogate(c)) {
        out.append(String.format("\\u%04x", (int) c));
      } else {
        out.append(c);
      }
    }
    out.append('"');
  }
}
