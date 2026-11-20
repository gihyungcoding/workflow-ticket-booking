package com.example.ticket_booking.service;

/** 텍스트 메일 한 통 — 받는 사람·제목·본문. HTML·첨부는 없다(요구사항 §7). */
public record MailMessage(String to, String subject, String body) {}
