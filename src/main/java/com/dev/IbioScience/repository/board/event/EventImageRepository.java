package com.dev.IbioScience.repository.board.event;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.board.event.EventImage;

public interface EventImageRepository extends JpaRepository<EventImage, Long> {

    List<EventImage> findAllByEventIdAndKind(Long eventId, EventImage.Kind kind);

    Optional<EventImage> findByEventIdAndKind(Long eventId, EventImage.Kind kind);

    void deleteAllByEventIdAndKind(Long eventId, EventImage.Kind kind);
}