package com.example.ticket_booking.config;

import com.example.ticket_booking.api.dto.ErrorResponse;
import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 접근 정책의 단일 지점(ADR-0013). 공연 API 는 permitAll 로 유지한다 — 소유권 검사는 이 태스크의 범위 밖이고 쓰기 가드(ADR-0017)가 그 사이의
 * 방어다. CORS 는 서블릿 필터 수준(CorsFilter, 최우선)이라 이 체인보다 먼저 처리된다(ADR-0016).
 */
@Configuration
public class SecurityConfig {

  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** JWT_SECRET 이 비었거나 짧으면 여기서 기동이 실패한다. */
  @Bean
  public JwtTokenProvider jwtTokenProvider(@Value("${JWT_SECRET:}") String secret, Clock clock) {
    return JwtTokenProvider.fromEnv(secret, clock);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtTokenProvider jwtTokenProvider,
      ObjectProvider<ClientRegistrationRepository> clientRegistrations,
      ObjectProvider<OAuth2AuthorizationRequestResolver> authorizationRequestResolver,
      ObjectProvider<OAuth2LoginSuccessHandler> successHandler,
      ObjectProvider<OAuth2LoginFailureHandler> failureHandler,
      ObjectProvider<OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>>
          tokenResponseClient)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable())
        .authorizeHttpRequests(
            authorize ->
                authorize
                    // 컨테이너가 sendError 로 처리하는 오류(타입 불일치 400·미지원 메서드 405·미디어 타입 415)는 /error 로 다시
                    // 디스패치된다 —
                    // 이것까지 인증을 요구하면 기존 오류 응답이 모두 401 로 바뀐다. 원래 요청의 인가는 REQUEST 디스패치에서 이미 끝났다.
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        "/api/performances/**",
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/api/auth/social/complete")
                    .permitAll()
                    // 소셜 로그인 시작·콜백 경로. 제공자가 등록되지 않은 경로가 401 이 아니라 404 로 떨어지게 열어 둔다
                    .requestMatchers("/oauth2/**", "/login/oauth2/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedEntryPoint()))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class);
    // 제공자가 하나도 설정되지 않았으면 OAuth2 로그인 필터를 아예 달지 않는다(이메일 인증만 동작)
    if (clientRegistrations.getIfAvailable() != null) {
      // loginPage 를 지정해 Spring 이 만드는 기본 로그인 화면(/login)을 열지 않는다 — 로그인 화면은 프론트에 있다
      http.oauth2Login(
          login ->
              login
                  .loginPage("/login")
                  .authorizationEndpoint(
                      endpoint ->
                          endpoint.authorizationRequestResolver(
                              authorizationRequestResolver.getObject()))
                  // 애플만 client_secret 을 요청마다 만들어야 해서, 그 빈이 있을 때만 토큰 교환 클라이언트를 바꾼다
                  .tokenEndpoint(
                      endpoint ->
                          tokenResponseClient.ifAvailable(
                              client -> endpoint.accessTokenResponseClient(client)))
                  .successHandler(successHandler.getObject())
                  .failureHandler(failureHandler.getObject()));
    }
    return http.build();
  }

  /** 인증이 필요한 경로에 유효한 토큰 없이 들어오면 기존 ErrorResponse 형식으로 401 을 돌려준다. */
  private static AuthenticationEntryPoint unauthorizedEntryPoint() {
    return (request, response, exception) -> {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType("application/json");
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response
          .getWriter()
          .write(
              OBJECT_MAPPER.writeValueAsString(
                  new ErrorResponse("UNAUTHORIZED", "인증이 필요합니다. 토큰이 없거나 올바르지 않습니다")));
    };
  }
}
