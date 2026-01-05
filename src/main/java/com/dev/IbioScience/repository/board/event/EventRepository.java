package com.dev.IbioScience.repository.board.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.dev.IbioScience.model.board.event.Event;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
}