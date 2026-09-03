package com.example.ticket_booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "performance")
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String venue;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "open_at")
    private Instant openAt;

    @Column(name = "close_at")
    private Instant closeAt;

    @Column(name = "total_seats")
    private Integer totalSeats;

    @Column(name = "available_seats")
    private Integer availableSeats;

    private Boolean cancelled;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Performance() {
    }

    public Performance(String title, String venue, Instant startAt, Instant openAt, Instant closeAt,
            Integer totalSeats, Integer availableSeats, Boolean cancelled) {
        this.title = title;
        this.venue = venue;
        this.startAt = startAt;
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.cancelled = cancelled;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getVenue() {
        return venue;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getOpenAt() {
        return openAt;
    }

    public Instant getCloseAt() {
        return closeAt;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public Boolean getCancelled() {
        return cancelled;
    }
}
