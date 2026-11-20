package com.example.ticket_booking.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 연결은 받지만 아무 응답도 보내지 않는 SMTP 자리(TASK-027 SC-22) — 클라이언트는 인사말을 기다리다 읽기 타임아웃에 걸린다. */
public final class BlackholeSmtpServer implements AutoCloseable {

  private final ServerSocket server;
  private final List<Socket> held = new CopyOnWriteArrayList<>();

  public BlackholeSmtpServer() {
    try {
      server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    Thread worker =
        new Thread(
            () -> {
              while (!server.isClosed()) {
                try {
                  held.add(server.accept());
                } catch (IOException closed) {
                  return;
                }
              }
            },
            "blackhole-smtp");
    worker.setDaemon(true);
    worker.start();
  }

  public int port() {
    return server.getLocalPort();
  }

  @Override
  public void close() {
    for (Socket socket : held) {
      try {
        socket.close();
      } catch (IOException ignored) {
        // 닫는 중
      }
    }
    try {
      server.close();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
