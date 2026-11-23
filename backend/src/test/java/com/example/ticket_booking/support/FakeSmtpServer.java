package com.example.ticket_booking.support;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 받은 메일을 모으는 최소 SMTP 서버(TASK-027 SC-02). 인증·TLS 없이 EHLO/HELO·MAIL·RCPT·DATA·RSET·NOOP·QUIT 만 답한다.
 * 받은 DATA 는 jakarta.mail 의 {@link MimeMessage} 로 파싱해 둔다 — 제목의 RFC 2047 인코딩(한글)을 그대로 풀기 위해서다.
 */
public final class FakeSmtpServer implements AutoCloseable {

  private final ServerSocket server;
  private final List<MimeMessage> received = new CopyOnWriteArrayList<>();
  private final Thread worker;

  public FakeSmtpServer() {
    try {
      server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    worker = new Thread(this::serve, "fake-smtp");
    worker.setDaemon(true);
    worker.start();
  }

  public int port() {
    return server.getLocalPort();
  }

  public List<MimeMessage> received() {
    return List.copyOf(received);
  }

  private void serve() {
    while (!server.isClosed()) {
      try (Socket socket = server.accept()) {
        handle(socket);
      } catch (IOException exception) {
        // 닫힘 또는 클라이언트 끊김 — 다음 연결을 받는다
      }
    }
  }

  private void handle(Socket socket) throws IOException {
    BufferedReader in =
        new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    OutputStream out = socket.getOutputStream();
    reply(out, "220 fake-smtp ready");
    String line;
    while ((line = in.readLine()) != null) {
      String command = line.length() >= 4 ? line.substring(0, 4).toUpperCase() : line.toUpperCase();
      switch (command) {
        case "EHLO", "HELO" -> reply(out, "250 fake-smtp");
        case "MAIL", "RCPT", "RSET", "NOOP" -> reply(out, "250 OK");
        case "DATA" -> {
          reply(out, "354 end with <CRLF>.<CRLF>");
          StringBuilder data = new StringBuilder();
          String dataLine;
          while ((dataLine = in.readLine()) != null && !dataLine.equals(".")) {
            data.append(dataLine.startsWith("..") ? dataLine.substring(1) : dataLine)
                .append("\r\n");
          }
          received.add(parse(data.toString()));
          reply(out, "250 OK queued");
        }
        case "QUIT" -> {
          reply(out, "221 bye");
          return;
        }
        default -> reply(out, "250 OK");
      }
    }
  }

  private static MimeMessage parse(String data) {
    try {
      return new MimeMessage(
          Session.getInstance(new Properties()),
          new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("SMTP DATA 를 메일로 읽지 못했다", exception);
    }
  }

  private static void reply(OutputStream out, String text) throws IOException {
    out.write((text + "\r\n").getBytes(StandardCharsets.UTF_8));
    out.flush();
  }

  @Override
  public void close() {
    try {
      server.close();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
