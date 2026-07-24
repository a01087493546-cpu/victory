package com.victory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.Response;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    List<Response> findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            Long studentId, String mode, String contentType, String stage);

    List<Response> findByModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            String mode, String contentType, String stage);

    List<Response> findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
            Long parentId, String mode, String contentType);

    List<Response> findByParent_IdAndModeAndContentTypeAndStudent_IdAndDeletedAtIsNullOrderByIdAsc(
            Long parentId, String mode, String contentType, Long studentId);
}
