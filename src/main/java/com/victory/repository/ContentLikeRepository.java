package com.victory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.ContentLike;

public interface ContentLikeRepository extends JpaRepository<ContentLike, Long> {

    long countByContentTypeAndContentId(String contentType, Long contentId);

    Optional<ContentLike> findByStudent_IdAndContentTypeAndContentId(
            Long studentId,
            String contentType,
            Long contentId);

    List<ContentLike> findByStudent_IdAndContentTypeAndContentIdIn(
            Long studentId,
            String contentType,
            Collection<Long> contentIds);

    void deleteByContentTypeAndContentId(String contentType, Long contentId);
}
