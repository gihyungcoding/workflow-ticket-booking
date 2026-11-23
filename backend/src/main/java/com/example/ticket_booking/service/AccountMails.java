package com.example.ticket_booking.service;

/**
 * 계정 메일 네 종류의 문구와 링크(요구사항 §3-4). 링크는 기동 때 검증·정리한 FRONTEND_BASE_URL 로만 만든다 — 요청의 Host·헤더를 읽지 않는다
 * (ADR-0032). 텍스트 메일 한 벌이다.
 */
public class AccountMails {

  private final String baseUrl;

  /**
   * @param baseUrl 검증·정리된 기준 주소(앞뒤 공백·끝 '/' 없음) — {@code config.FrontendBaseUrl} 이 만든다
   */
  public AccountMails(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public MailMessage verification(String to, String rawToken) {
    return new MailMessage(
        to,
        "이메일 인증을 해 주세요",
        "티켓온 가입을 마치려면 아래 링크를 눌러 이메일을 인증해 주세요.\n\n"
            + baseUrl
            + "/verify-email#token="
            + rawToken
            + "\n\n링크는 24시간 안에 눌러 주세요.\n직접 가입하지 않았다면 무시해 주세요.");
  }

  public MailMessage passwordReset(String to, String rawToken) {
    return new MailMessage(
        to,
        "비밀번호를 다시 정해 주세요",
        "아래 링크를 눌러 새 비밀번호를 정해 주세요.\n\n"
            + baseUrl
            + "/reset-password#token="
            + rawToken
            + "\n\n링크는 30분 안에 눌러 주세요.\n요청하지 않았다면 무시해 주세요.");
  }

  public MailMessage passwordChanged(String to) {
    return new MailMessage(
        to,
        "비밀번호를 바꿨어요",
        "티켓온 계정의 비밀번호가 방금 바뀌었어요.\n\n" + "본인이 바꾼 것이 아니라면 로그인 화면의 비밀번호 찾기로 다시 정해 주세요.");
  }

  public MailMessage socialAccountNotice(String to) {
    return new MailMessage(
        to,
        "소셜 계정으로 로그인해 주세요",
        "이 이메일은 카카오·구글 등 소셜 계정으로 가입돼 있어 비밀번호가 없어요.\n"
            + "가입한 소셜 계정으로 로그인해 주세요.\n\n"
            + baseUrl
            + "/login");
  }
}
