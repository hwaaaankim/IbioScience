package com.dev.IbioScience.repository.product.register;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.util.Keyword;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    Optional<Keyword> findByWord(String word);
}

