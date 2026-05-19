package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findBySlug(String slug);
    Optional<Tag> findByName(String name);
    boolean existsBySlug(String slug);
    List<Tag> findAllByOrderByNameAsc();
}
